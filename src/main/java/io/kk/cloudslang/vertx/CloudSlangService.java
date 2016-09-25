package io.kk.cloudslang.vertx;

import io.cloudslang.lang.api.Slang;
import io.cloudslang.lang.compiler.SlangSource;
import io.cloudslang.lang.entities.SystemProperty;
import io.cloudslang.score.events.ScoreEvent;
import io.kk.cloudslang.vertx.util.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * @author keke
 */
public class CloudSlangService extends AbstractVerticle {
  public static final String EXEC_FLOW = CloudSlangService.class + ".execflow";
  public static final String EXEC_FLOW_RESPONSE = EXEC_FLOW + "_response";
  public static final String SLANG_EXECUTION_EXCEPTION = "SLANG_EXECUTION_EXCEPTION";
  private static final String EVENT_EXECUTION_FINISHED = "EVENT_EXECUTION_FINISHED";
  private static final String EXECUTION_ID = "EXECUTION_ID";
  private static final Logger LOG = LoggerFactory.getLogger(CloudSlangService.class);
  private Slang slang;
  private File contentPath;
  private List<DependenciesLoader> loaders = new ArrayList<>();
  private List<SystemPropertiesLoader> spLoaders = new ArrayList<>();

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    loadLoaders("loaders", loaders);
    loadLoaders("systemPropertiesLoader", spLoaders);

    vertx.eventBus().registerDefaultCodec(FlowParams.class, new FlowParamsCodec());
    vertx.eventBus().registerDefaultCodec(ExecResponse.class, new ExecResponseCodec());
    contentPath = new File(getContentPath()).getAbsoluteFile().getCanonicalFile();
    if (!contentPath.exists() && !contentPath.isDirectory()) {
      LOG.error("Can not find Content Path at {}", contentPath);
      startFuture.fail(contentPath + " not existed");
    } else {
      LOG.info("CloudSlang content path is {}", contentPath);
      initCloudSlang();
      vertx.eventBus().consumer(EXEC_FLOW, this::execFlow);
      super.start(startFuture);
      LOG.info("CloudSlang service Deployed");
    }
  }

  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }

  private <T> void loadLoaders(String name, List<T> list) {
    config().getJsonArray(name, new JsonArray()).forEach(l -> {
      try {
        list.add((T) Class.forName(l.toString()).newInstance());
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        LOG.warn("Unable to load loader " + l, e);
      }
    });
  }

  private void initCloudSlang() {
    ApplicationContext applicationContext =
        new ClassPathXmlApplicationContext("/META-INF/spring/context.xml");
    slang = applicationContext.getBean(Slang.class);
    slang.subscribeOnAllEvents(this::handleCloudSlangEvent);
  }

  private void handleCloudSlangEvent(ScoreEvent scoreEvent) {
    LOG.debug("CS event: {}", scoreEvent.getEventType());
    String type = scoreEvent.getEventType();
    if (LOG.isDebugEnabled()) {
//      CS event: SLANG_EXECUTION_EXCEPTION
//      1474815791619- [DEBUG] 2016-09-25 23:03:11.619 [scoreWorkerScheduler-3] i.k.c.v.CloudSlangService - CS event: SCORE_FINISHED_EVENT
      if (scoreEvent.getEventType().equals("SCORE_FINISHED_EVENT")) {
        LOG.debug("Score finished event={} - data={}", scoreEvent.getEventType(), scoreEvent.getData());
      }
      if (scoreEvent.getEventType().equals("SLANG_EXECUTION_EXCEPTION")) {
        LOG.debug("Score finished event={} - data={}", scoreEvent.getEventType(), scoreEvent.getData());
      }
    }
    if (type.equals(EVENT_EXECUTION_FINISHED)) {
      Map<String, Object> dataMap = (Map<String, Object>) scoreEvent.getData();
      long execId = ((Long) dataMap.get(EXECUTION_ID));
      String result = dataMap.get("RESULT").toString();
      ExecResponse response = new ExecResponse();
      response.setStatus(result);
      if (response.getStatus().equals("success")) {
        Map<String, Object> outputs = (Map<String, Object>) dataMap.get("OUTPUTS");
        response.setResult(new JsonObject(outputs));
      }
      vertx.eventBus().send(execId + ".finished", response);
    } else if (type.equals(SLANG_EXECUTION_EXCEPTION)) {
      Map<String, Object> dataMap = (Map<String, Object>) scoreEvent.getData();
      long execId = ((Long) dataMap.get(EXECUTION_ID));
//      String result = dataMap.get("RESULT").toString();
      ExecResponse response = new ExecResponse();
      response.setStatus("error");
      response.setErrorReason(dataMap.get("EXCEPTION").toString());
      vertx.eventBus().send(execId + ".finished", response);
    }
  }

  private String getContentPath() {
    return Util.getConfig(config(), "CONTENT_PATH", "contentPath");
  }

  private void execFlow(Message<FlowParams> message) {
    FlowParams params = message.body();
    if (LOG.isDebugEnabled())
      LOG.debug("To execute a flow {}", params.toJson().encodePrettily());
    String name = params.getFlowName();
    String address = EXEC_FLOW_RESPONSE + "_" + params.getExecId();
    File file = getContentFile(name, "orc.json");
    LOG.debug("Orchestration {} entry is {}", name, file);
    if (!file.exists()) {
      LOG.warn("Flow {} is not found", name);
      ExecResponse response = new ExecResponse();
      response.setStatus("error");
      response.setStatusCode(500);
      response.setErrorReason("File " + name + " not found");
      vertx.eventBus().publish(address, response);
    } else {
      try {
        JsonObject flowCfg = new JsonObject(FileUtils.readFileToString(file, "UTF-8"));
        if (LOG.isDebugEnabled())
          LOG.debug("Flow [{}] config - {}", name, flowCfg.encodePrettily());
        long slangId = slang.compileAndRun(getSource(name, flowCfg), getDependencies(name, flowCfg),
            getInputs(params.getArgs()), getSystemProperties());
        MessageConsumer<ExecResponse> consumer = vertx.eventBus().localConsumer(slangId + ".finished");
        consumer.handler((Message<ExecResponse> h) -> {
          if (LOG.isDebugEnabled())
            LOG.debug("Result of execution - {}", h.body().toJson().encodePrettily());
          ExecResponse response = h.body();
          if (response.getStatus().equals("success")) {
            response.setStatusCode(200);
          } else {
            response.setStatusCode(500);
          }
          vertx.eventBus().publish(address, response);
          consumer.unregister();
        });
        consumer.exceptionHandler(ex -> {
          LOG.warn("Error when executing " + name, ex);
          ExecResponse response = new ExecResponse();
          response.setStatus("error");
          response.setStatusCode(500);
          response.setErrorReason(ex.toString() + " - " + ex.getMessage());
          vertx.eventBus().publish(address, response);
          consumer.unregister();
        });
      } catch (Exception e) {
        LOG.error("Unable to execute flow {}", name);
        LOG.error("Unable to execute flow", e);
        ExecResponse response = new ExecResponse();
        response.setStatus("error");
        response.setStatusCode(500);
        response.setErrorReason(e.toString() + " - " + e.getMessage());
        vertx.eventBus().publish(address, response);
      }
    }
  }


  private Set<SystemProperty> getSystemProperties() {
    Set<SystemProperty> props = new HashSet<>();
    spLoaders.forEach(l -> {
      props.addAll(l.load(config()));
    });
    return props;
  }

  private Map<String, ? extends Serializable> getInputs(JsonObject args) {
    Map<String, String> map = new HashMap<>();
    map.put("args", args.toString());
    return map;
  }

  private Set<SlangSource> getDependencies(String name, JsonObject cfg) {
    LOG.debug("To load dependencies");
    Set<SlangSource> depSet = new HashSet<>();
    cfg.getJsonArray("dependencies").forEach(o -> {
      String dep = o.toString();
      if (dep.trim().endsWith("/")) {
        LOG.info("Load dependencies from a folder {}", dep);
        File folder = new File(contentPath + "/" + name, dep);
        FileUtils.listFiles(folder, new String[]{"sl"}, false).forEach(file -> {
          depSet.add(SlangSource.fromFile(file));
        });
      } else {
        depSet.add(SlangSource.fromFile(getContentFile(name, dep)));
      }
    });
    loaders.forEach(l -> {
      depSet.addAll(l.load(config()));
    });
    return depSet;
  }

  private SlangSource getSource(String orcName, JsonObject cfg) {
    return SlangSource.fromFile(getContentFile(orcName, cfg.getString("source", "main.sl")));
  }

  private File getContentFile(String orcName, String fileName) {
    return new File(contentPath, orcName + "/" + fileName);
  }
}
