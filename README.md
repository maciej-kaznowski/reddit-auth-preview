# reddit-auth-preview
This repository contains a few modules for authentication/authorisation with the reddit API from one of my apps - [React for Reddit](https://play.google.com/store/apps/details?id=com.innercirclesoftware.reddit&hl=en&gl=US). It won't compile as as not all dependencies are provided. It is meant for demonstration purposes only.


Some key points:

- All modules are written in kotlin (in these cases compiles down to JVM bytecode)
- It is split into 3 different modules:
  - API: any module can use this
  - core: used by the main module in the app
  - tests: other modules can use this in tests to easily create API objects
- Unfortunately it is not a pure JVM module - it depends on another module (not included) which has android dependencies. The goal one day is to turn these into pure JVM modules
- All the code is fairly basic and well tested
- Uses quite a bit of RxJava for reactive, functional code
