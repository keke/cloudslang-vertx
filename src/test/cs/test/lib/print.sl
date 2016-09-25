namespace: test

operation:

  name: print

  inputs:
    - input

  python_action:
    script: |
      print input
