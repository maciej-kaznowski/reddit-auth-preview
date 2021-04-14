# reddit-auth-preview
This repository contains a few modules for authentication/authorisation with the reddit API from one of my apps - [React for Reddit](https://play.google.com/store/apps/details?id=com.innercirclesoftware.reddit&hl=en&gl=US). It won't compile as not all dependencies are provided. It is meant for demonstration purposes only.


Some key points:

- This is not currently meant to be used in a backend - they are being used in an Android app. But I do one day intend to allow some modules to be used for some backend projects of mine.
- All modules are written in kotlin (in these cases compiles down to JVM bytecode). 
- It is split into 3 different modules:
  - API: any module can use this
  - core: used by the main module in the app, depends on API module
  - tests: other modules can use this in tests to easily create API objects, depends on API module
- Unfortunately they are not pure JVM modules - they depend on another module (not included) which has android dependencies. The goal one day is to turn these into pure JVM modules to allow greater extensibility.
- All the code is fairly basic and well tested
- Uses quite a bit of RxJava for reactive, functional code - mainly due to the architecture of the app using these modules
- The tests use one of my public repositories to help with creating random test data (strings, collections, primitives etc): https://github.com/maciej-kaznowski/randoms (that repository is not finished but does 90% of what I need)
- The code was written in February 2021


Interesting classes:
- https://github.com/maciej-kaznowski/reddit-auth-api/src/main/java/com/innercirclesoftware/reddit_auth_api/RedditAuthenticator.kt RedditAuthenticator - any part of the app which authenticates with reddit does so through this interface
- https://github.com/maciej-kaznowski/reddit-auth/src/main/java/com/innercirclesoftware/reddit_auth/RedditAuthenticatorImpl.kt RedditAuthenticatorImpl - the implementation of the above class
- https://github.com/maciej-kaznowski/reddit-auth/src/test/java/com/innercirclesoftware/reddit_auth/RedditAuthenticatorImplTest.kt unit tests for the above
- https://github.com/maciej-kaznowski/reddit-auth-preview/blob/main/reddit-auth/src/androidTest/java/com/innercirclesoftware/reddit_auth/RedditAuthenticatorImplIntTest.kt - integration test for the entire authentication flow
