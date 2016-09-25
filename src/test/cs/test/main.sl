namespace: test

flow:
  name: main

  inputs:
    - args

  workflow:
    - print:
        do:
          print:
            - input: ${args}
