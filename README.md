Tracing
=======

Spring Boot Actuator provides dependency management and auto-configuration for [Micrometer Tracing](https://docs.micrometer.io/tracing/reference/1.3), a facade for popular tracer libraries.

To learn more about Micrometer Tracing capabilities, see its [reference documentation](https://docs.micrometer.io/tracing/reference/1.3).

[](#actuator.micrometer-tracing.tracers)Supported Tracers
---------------------------------------------------------

Spring Boot ships auto-configuration for the following tracers:

*   [OpenTelemetry](https://opentelemetry.io/) with [Zipkin](https://zipkin.io/), [Wavefront](https://docs.wavefront.com/), or [OTLP](https://opentelemetry.io/docs/reference/specification/protocol/)
    
*   [OpenZipkin Brave](https://github.com/openzipkin/brave) with [Zipkin](https://zipkin.io/) or [Wavefront](https://docs.wavefront.com/)
    

[](#actuator.micrometer-tracing.getting-started)Getting Started
---------------------------------------------------------------

We need an example application that we can use to get started with tracing. For our purposes, the simple “Hello World!” web application that’s covered in the [Developing Your First Spring Boot Application](../../tutorial/first-application/index.html) section will suffice. We’re going to use the OpenTelemetry tracer with Zipkin as trace backend.

To recap, our main application code looks like this:

    import org.apache.commons.logging.Log;
    import org.apache.commons.logging.LogFactory;
    
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    
    @RestController
    @SpringBootApplication
    public class MyApplication {
    
    	private static final Log logger = LogFactory.getLog(MyApplication.class);
    
    	@RequestMapping("/")
    	String home() {
    		logger.info("home() has been called");
    		return "Hello World!";
    	}
    
    	public static void main(String[] args) {
    		SpringApplication.run(MyApplication.class, args);
    	}
    
    }

There’s an added logger statement in the `home()` method, which will be important later.

Now we have to add the following dependencies:

*   `org.springframework.boot:spring-boot-starter-actuator`
    
*   `io.micrometer:micrometer-tracing-bridge-otel` - bridges the Micrometer Observation API to OpenTelemetry.
    
*   `io.opentelemetry:opentelemetry-exporter-zipkin` - reports [traces](https://docs.micrometer.io/tracing/reference/1.3/glossary) to Zipkin.
    

Add the following application properties:

*   Properties
    
*   YAML
    

    management.tracing.sampling.probability=1

    management:
      tracing:
        sampling:
          probability: 1.0

By default, Spring Boot samples only 10% of requests to prevent overwhelming the trace backend. This property switches it to 100% so that every request is sent to the trace backend.

To collect and visualize the traces, we need a running trace backend. We use Zipkin as our trace backend here. The [Zipkin Quickstart guide](https://zipkin.io/pages/quickstart) provides instructions how to start Zipkin locally.

After Zipkin is running, you can start your application.

If you open a web browser to `[localhost:8080](http://localhost:8080)`, you should see the following output:

    Hello World!

Behind the scenes, an observation has been created for the HTTP request, which in turn gets bridged to OpenTelemetry, which reports a new trace to Zipkin.

Now open the Zipkin UI at `[localhost:9411](http://localhost:9411)` and press the "Run Query" button to list all collected traces. You should see one trace. Press the "Show" button to see the details of that trace.

[](#actuator.micrometer-tracing.logging)Logging Correlation IDs
---------------------------------------------------------------

Correlation IDs provide a helpful way to link lines in your log files to spans/traces. If you are using Micrometer Tracing, Spring Boot will include correlation IDs in your logs by default.

The default correlation ID is built from `traceId` and `spanId` [MDC](https://logback.qos.ch/manual/mdc.html) values. For example, if Micrometer Tracing has added an MDC `traceId` of `803B448A0489F84084905D3093480352` and an MDC `spanId` of `3425F23BB2432450` the log output will include the correlation ID `[803B448A0489F84084905D3093480352-3425F23BB2432450]`.

If you prefer to use a different format for your correlation ID, you can use the `logging.pattern.correlation` property to define one. For example, the following will provide a correlation ID for Logback in format previously used by Spring Cloud Sleuth:

*   Properties
    
*   YAML
    

    logging.pattern.correlation=[${spring.application.name:},%X{traceId:-},%X{spanId:-}] 
    logging.include-application-name=false

    logging:
      pattern:
        correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
      include-application-name: false

In the example above, `logging.include-application-name` is set to `false` to avoid the application name being duplicated in the log messages (`logging.pattern.correlation` already contains it). It’s also worth mentioning that `logging.pattern.correlation` contains a trailing space so that it is separated from the logger name that comes right after it by default.

[](#actuator.micrometer-tracing.propagating-traces)Propagating Traces
---------------------------------------------------------------------

To automatically propagate traces over the network, use the auto-configured [`RestTemplateBuilder`](../io/rest-client.html#io.rest-client.resttemplate), [`RestClient.Builder`](../io/rest-client.html#io.rest-client.restclient) or [`WebClient.Builder`](../io/rest-client.html#io.rest-client.webclient) to construct the client.

If you create the `RestTemplate`, the `RestClient` or the `WebClient` without using the auto-configured builders, automatic trace propagation won’t work!

[](#actuator.micrometer-tracing.tracer-implementations)Tracer Implementations
-----------------------------------------------------------------------------

As Micrometer Tracer supports multiple tracer implementations, there are multiple dependency combinations possible with Spring Boot.

All tracer implementations need the `org.springframework.boot:spring-boot-starter-actuator` dependency.

### [](#actuator.micrometer-tracing.tracer-implementations.otel-zipkin)OpenTelemetry With Zipkin

Tracing with OpenTelemetry and reporting to Zipkin requires the following dependencies:

*   `io.micrometer:micrometer-tracing-bridge-otel` - bridges the Micrometer Observation API to OpenTelemetry.
    
*   `io.opentelemetry:opentelemetry-exporter-zipkin` - reports traces to Zipkin.
    

Use the `management.zipkin.tracing.*` configuration properties to configure reporting to Zipkin.

### [](#actuator.micrometer-tracing.tracer-implementations.otel-wavefront)OpenTelemetry With Wavefront

Tracing with OpenTelemetry and reporting to Wavefront requires the following dependencies:

*   `io.micrometer:micrometer-tracing-bridge-otel` - bridges the Micrometer Observation API to OpenTelemetry.
    
*   `io.micrometer:micrometer-tracing-reporter-wavefront` - reports traces to Wavefront.
    

Use the `management.wavefront.*` configuration properties to configure reporting to Wavefront.

### [](#actuator.micrometer-tracing.tracer-implementations.otel-otlp)OpenTelemetry With OTLP

Tracing with OpenTelemetry and reporting using OTLP requires the following dependencies:

*   `io.micrometer:micrometer-tracing-bridge-otel` - bridges the Micrometer Observation API to OpenTelemetry.
    
*   `io.opentelemetry:opentelemetry-exporter-otlp` - reports traces to a collector that can accept OTLP.
    

Use the `management.otlp.tracing.*` configuration properties to configure reporting using OTLP.

### [](#actuator.micrometer-tracing.tracer-implementations.brave-zipkin)OpenZipkin Brave With Zipkin

Tracing with OpenZipkin Brave and reporting to Zipkin requires the following dependencies:

*   `io.micrometer:micrometer-tracing-bridge-brave` - bridges the Micrometer Observation API to Brave.
    
*   `io.zipkin.reporter2:zipkin-reporter-brave` - reports traces to Zipkin.
    

Use the `management.zipkin.tracing.*` configuration properties to configure reporting to Zipkin.

### [](#actuator.micrometer-tracing.tracer-implementations.brave-wavefront)OpenZipkin Brave With Wavefront

Tracing with OpenZipkin Brave and reporting to Wavefront requires the following dependencies:

*   `io.micrometer:micrometer-tracing-bridge-brave` - bridges the Micrometer Observation API to Brave.
    
*   `io.micrometer:micrometer-tracing-reporter-wavefront` - reports traces to Wavefront.
    

Use the `management.wavefront.*` configuration properties to configure reporting to Wavefront.

[](#actuator.micrometer-tracing.micrometer-observation)Integration with Micrometer Observation
----------------------------------------------------------------------------------------------

A `TracingAwareMeterObservationHandler` is automatically registered on the `ObservationRegistry`, which creates spans for every completed observation.

[](#actuator.micrometer-tracing.creating-spans)Creating Custom Spans
--------------------------------------------------------------------

You can create your own spans by starting an observation. For this, inject `ObservationRegistry` into your component:

    import io.micrometer.observation.Observation;
    import io.micrometer.observation.ObservationRegistry;
    
    import org.springframework.stereotype.Component;
    
    @Component
    class CustomObservation {
    
    	private final ObservationRegistry observationRegistry;
    
    	CustomObservation(ObservationRegistry observationRegistry) {
    		this.observationRegistry = observationRegistry;
    	}
    
    	void someOperation() {
    		Observation observation = Observation.createNotStarted("some-operation", this.observationRegistry);
    		observation.lowCardinalityKeyValue("some-tag", "some-value");
    		observation.observe(() -> {
    			// Business logic ...
    		});
    	}
    
    }

This will create an observation named "some-operation" with the tag "some-tag=some-value".

If you want to create a span without creating a metric, you need to use the [lower-level `Tracer` API](https://docs.micrometer.io/tracing/reference/1.3/api) from Micrometer.

[](#actuator.micrometer-tracing.baggage)Baggage
-----------------------------------------------

You can create baggage with the `Tracer` API:

    import io.micrometer.tracing.BaggageInScope;
    import io.micrometer.tracing.Tracer;
    
    import org.springframework.stereotype.Component;
    
    @Component
    class CreatingBaggage {
    
    	private final Tracer tracer;
    
    	CreatingBaggage(Tracer tracer) {
    		this.tracer = tracer;
    	}
    
    	void doSomething() {
    		try (BaggageInScope scope = this.tracer.createBaggageInScope("baggage1", "value1")) {
    			// Business logic
    		}
    	}
    
    }

This example creates baggage named `baggage1` with the value `value1`. The baggage is automatically propagated over the network if you’re using W3C propagation. If you’re using B3 propagation, baggage is not automatically propagated. To manually propagate baggage over the network, use the `management.tracing.baggage.remote-fields` configuration property (this works for W3C, too). For the example above, setting this property to `baggage1` results in an HTTP header `baggage1: value1`.

If you want to propagate the baggage to the MDC, use the `management.tracing.baggage.correlation.fields` configuration property. For the example above, setting this property to `baggage1` results in an MDC entry named `baggage1`.

[](#actuator.micrometer-tracing.tests)Tests
-------------------------------------------

Tracing components which are reporting data are not auto-configured when using `@SpringBootTest`. See [Using Tracing](../testing/spring-boot-applications.html#testing.spring-boot-applications.tracing) for more details.

Run Example
-------------------------------------------
1. Run the Zipkin server.

    docker compose up
2. Start the service1 microservice.

    cd service1/

   ./mvnw spring-boot:run
3. Start the service2 microservice.

    cd service2/

    ./mvnw spring-boot:run

curl http://localhost:8080/service1/hello

open zipkin http://localhost:9411/