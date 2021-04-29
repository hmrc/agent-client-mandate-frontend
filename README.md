agent-client-mandate-frontend
=============================

[![Build Status](https://travis-ci.org/hmrc/agent-client-mandate-frontend.svg)](https://travis-ci.org/hmrc/agent-client-mandate-frontend)

This frontend microservice is used to create or break a relationship between an agent and a client for a given service. 
The relationship created is in the form of contract called mandate which has details of client & agent among few other things. 

Once the mandate has been agreed it's stored in a datastore and ensures that the agent can work on behalf of the client for that service.

Mandate life cycle
------------------
![mandate1](https://user-images.githubusercontent.com/13600497/31657897-81b0041a-b327-11e7-8d73-4acec0f5bbe7.png)

## Summary

### Useful Pages for external services to link to

| PATH | Supported Methods |.conf
|------|-------------------|
| ```/mandate/agent/service ``` | Shows a list of services that the agent can create mandates for |
| ```/mandate/agent/summary/:service``` | Shows any pending or current clients for a given service. Also has a link to create a mandate for a new client |
| ```/mandate/email/:service``` | Starts the process of creating a mandate for a new client |
| ```/mandate/client/email``` | Starts the process for the client to accept a mandate |


## Adding a new service

### First service to be added after ATED
Note: If this is the first service to be added after ATED then the feature switch at MandateFeatureSwitches.singleService in FeatureSwitches will have to be removed.
This feature switch causes the ```/mandate/agent/service ``` to be skipped and go straight to the ATED summary page.

### Adding a new service to the view
Update the page selectService.scala.html to add any new services that the agent can choose from.

### Update the application.conf to add links back to the new service
Ensure that you update application.conf with the links back to the dev version of your service.
i.e.

```
  delegated-service-redirect-url {
    ated = "http://localhost:9916/ated/account-summary"
  }

  delegated-service-home-url {
    ated = "http://localhost:9916/ated/welcome"
  }
```

| Property | Description |
|------|-------------------|
| delegated-service-redirect-url | The page that the agent will return to when they click on a client after it's accepted the mandate |
| delegated-service-home-url | After the agent has created a mandated they're told to get the client to visit this page to log in and accept it |

Requirements
------------

This service is written in [Scala] and [Play], so needs the latest [JRE] to run.


Authentication
------------

This user logs into this service using the [Government Gateway]


Acronyms
--------

In the context of this service we use the following acronyms:

* [API]: Application Programming Interface

* [HoD]: Head of Duty

* [JRE]: Java Runtime Environment

* [JSON]: JavaScript Object Notation

* [URL]: Uniform Resource Locator

License
-------

This code is open source software licensed under the [Apache 2.0 License].

[Scala]: http://www.scala-lang.org/
[Play]: http://playframework.com/
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html

[Government Gateway]: http://www.gateway.gov.uk/

[API]: https://en.wikipedia.org/wiki/Application_programming_interface
[HoD]: http://webarchive.nationalarchives.gov.uk/+/http://www.hmrc.gov.uk/manuals/sam/samglossary/samgloss249.htm
[JSON]: http://json.org/
[URL]: https://en.wikipedia.org/wiki/Uniform_Resource_Locator

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html
