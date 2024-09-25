# A Simple Solution for Log Centralization Using Spring and RabbitMQ
You’ll set up a new microservice to aggregate logs from all your Spring Boot applications. 
To keep it simple, it won’t have a data layer to persist logs; it’ll just receive log lines from 
other services and print them together to the standard output. This basic solution will 
serve to demonstrate this pattern and the next one, distributed tracing.

To channel the log outputs, you’ll use a tool you already have in your system and is 
perfect for that purpose: RabbitMQ. To capture each logged line in the applications and 
send them as RabbitMQ messages, you’ll benefit from Logback (https://logback
.qos.ch/), the logger implementation you’ve been using within Spring Boot. Logback 
is a logging framework for Java that implements SL4J (Simple Logging Facade for Java). 
Given that this tool is driven by an external configuration file, you don’t need to modify 
the code in your applications.

In Logback, the piece of logic that writes a log line to the specific destination is called 
an appender. This logging library includes some built-in appenders to print messages 
to the console (ConsoleAppender) or files (FileAppender and RollingFileAppender). 
You didn’t need to configure them because Spring Boot includes some default Logback 
configuration within its dependencies and also sets up the printed message patterns.

The good news is that Spring AMQP provides a Logback AMQP logging appender 
that does exactly what you need: it takes each log line and produces a message to a given 
exchange in RabbitMQ, with a format and some extra options that you can customize.
First, let’s prepare the Logback configuration you need to add to your applications. 
Spring Boot allows you to extend the defaults by creating a file named logback-spring.
xml in the application resources folder (src/main/resources), which will be picked up 
automatically upon application initialization. See Listing 8-41. In this file, you import 
the existing default values and create and set a new appender for all messages that have 
level INFO or higher. The AMQP appender documentation (https://docs.spring.io/
spring-amqp/docs/current/reference/html/#logging) lists all parameters and their 
meanings; let’s look at the ones you need.
* applicationId: Set it to the application name so you can distinguish 
the source when you aggregate logs.
* host: This is the host where RabbitMQ is running. Since it can be 
different per environment, you’ll connect this value to the 
spring.rabbitmq.host Spring property. Spring allows you to do this 
via the springProperty tag. You give this Logback property a name, 
rabbitMQHost, and you use the ${rabbitMQHost:-localhost} syntax 
to either use the property value if it’s set or use the default localhost
(defaults are set with the :- separator).
* routingKeyPattern: This is the routing key per message, which you 
set to a concatenation of the applicationId and level (notated with 
%p) for more flexibility if you want to filter on the consumer side.
* exchangeName: Specify the name of the exchange in RabbitMQ to 
publish messages. It’ll be a topic exchange by default, so you can call 
it logs.topic.
* declareExchange: Set it to true to create the exchange if it’s not 
there yet.
* durable: Also set this to true so the exchange survives server restarts.
* deliveryMode: Make it PERSISTENT so log messages are stored until 
they’re consumed by the aggregator.
* generateId: Set it to true so each message will have a unique 
identifier.
* charset: It’s a good practice to set it to UTF-8 to make sure all parties 
use the same encoding.

Listing 8-41 shows the full contents of the logback-spring.xml file in the 
Gamification project. Note how you’re adding a layout with a custom pattern to your 
new appender. This way, you can encode your messages including not only the message 
(%msg) but also some extra information like the time (%d{HH:mm:ss.SSS}), the thread 
name ([%t]), and the logger class (%logger{36}). If you’re curious about the pattern 
notation, check out the Logback’s reference docs (https://logback.qos.ch/manual/
layouts.html#conversionWord). The last part of the file configures the root logger (the 
default one) to use both the CONSOLE appender, defined in one of the included files, and 
the newly defined AMQP appender.

```xml
<configuration>
    <include resource="org/springframework/boot/logging/logback/
    defaults.xml" />
    <include resource="org/springframework/boot/logging/logback/console-
    appender.xml" />
    <springProperty scope="context" name="rabbitMQHost" source="spring.
    rabbitmq.host"/>
    <appender name="AMQP"
    class="org.springframework.amqp.rabbit.logback.AmqpAppender">
        <layout>
            <pattern>%d{HH:mm:ss.SSS} [%t] %logger{36} - %msg</pattern>
        </layout>
        <applicationId>gamification</applicationId>
        <host>${rabbitMQHost:-localhost}</host>
        <routingKeyPattern>%property{applicationId}.%p</routingKeyPattern>
        <exchangeName>logs.topic</exchangeName>
        <declareExchange>true</declareExchange>
        <durable>true</durable>
        <deliveryMode>PERSISTENT</deliveryMode>
        <generateId>true</generateId>
        <charset>UTF-8</charset>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="AMQP" />
    </root>
</configuration>
```
You have to make sure you add this file to the three Spring Boot projects you have: 
Multiplication, Gamification, and Gateway. In each one of them, you must change the 
applicationId value accordingly.
In addition to this basic setup of log producers, you can adjust the log level for the 
class that the appender uses to connect to RabbitMQ as WARN. This is an optional step, 
but it avoids hundreds of logs when the RabbitMQ server is not available (e.g., while 
starting up your system). Since the appender is configured during the bootstrap phase, 
you need to add this configuration setting to the corresponding bootstrap.properties
and boostrap.yml files, depending on the project. See Listings 8-42 and 8-43.

```console
logging.level.org.springframework.amqp.rabbit.connection.
CachingConnectionFactory = WARN
```
The next time you start your applications, all logs will be output not only to the 
console but also as messages produced to the logs.topic exchange in RabbitMQ. You 
can verify that by accessing the RabbitMQ Web UI at localhost:15672

## Consuming Logs and Printing Them
Now that you have all logs together published to an exchange, you’ll build the consumer 
side: a new microservice that consumes all these messages and outputs them together.

First, navigate to the Spring Initializr site start.spring.io (https://start.spring.io/) 
and create a logs project using the same setup as you chose for other applications: Maven 
and JDK 17. In the list of dependencies, add Spring for RabbitMQ, Spring Web, Validation, 
Spring Boot Actuator, Lombok, and Consul Configuration. Note that you don’t need to make 
this service discoverable, so don’t add Consul Discovery

Once you import this project into your workspace, you can add some configuration 
to make it possible to connect to the configuration server. You’re not going to add any 
specific configuration for now, but it’s good to do this to make it consistent with the 
rest of the microservices. In the main/src/resources folder, copy the contents of the 
bootstrap.properties file you included in other projects. Set the application name and 
a dedicated port in the application.properties file as well. See Listing 8-44.

```console
spring.application.name=logs
server.port=8580
```

You need a Spring Boot configuration class to declare the exchange, the queue where 
you want to consume the messages from, and the binding object to attach the queue 
to the topic exchange with a binding key pattern to consume all of them containing the 
special character (#). See Listing 8-45. Remember that since you added the logging level 
to the routing keys, you can also adjust this value to get only errors, for example. Anyway, 
in this case, subscribe to all messages (#).
package microservices.book.logs;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
```java
@Configuration
public class AMQPConfiguration {
    @Bean
    public TopicExchange logsExchange() {
        return ExchangeBuilder.topicExchange("logs.topic")
        .durable(true)
        .build();
    }

    @Bean
    public Queue logsQueue() {
        return QueueBuilder.durable("logs.queue").build();
    }

    @Bean
    public Binding logsBinding(final Queue logsQueue,
        final TopicExchange logsExchange) {
        return BindingBuilder.bind(logsQueue)
        .to(logsExchange).with("#");
    }
}
```
The next step is to create a simple service with the @RabbitListener that maps 
the logging level of the received messages, passed as a RabbitMQ message header, to a 
logging level in the Logs microservice, using the corresponding log.info(), 
log.error(), or log.warn(). Note that you use the @Header annotation here to extract 
AMQP headers as method arguments. You also use a logging Marker to add the 
application name (appId) to the log line without needing to concatenate it as part of the 
message. This is a flexible way in the SLF4J standard to add contextual values to logs
```java
package microservices.book.logs;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class LogsConsumer {
    @RabbitListener(queues = "logs.queue")
    public void log(final String msg,
    @Header("level") String level,
    @Header("amqp_appId") String appId) {
        Marker marker = MarkerFactory.getMarker(appId);
        switch (level) {
            case "INFO" -> log.info(marker, msg);
            case "ERROR" -> log.error(marker, msg);
            case "WARN" -> log.warn(marker, msg);
        }
    }
}
```
Finally, customize the log output produced by this new microservice. Since it’ll 
aggregate multiple logs from different services, the most relevant property is the 
application name. You must override the Spring Boot defaults this time and define a 
simple format in a logback-spring.xml file for the CONSOLE appender that outputs the 
marker, the level, and the message
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>
            [%-15marker] %highlight(%-5level) %msg%n
            </Pattern>
        </layout>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```
That’s all the code you need in this new project. Now you can build the sources and 
start this new microservice with the rest of the components in your system.
1. Run the RabbitMQ server.

    docker compose up
1. Start the service1 microservice.

    cd service1/

   ./mvnw spring-boot:run
2. Start the Logs microservice.

    cd logs/

    ./mvnw spring-boot:run

Once you start this new microservice, it’ll consume all log messages produced by the 
other applications. To see that in practice, 

curl http://localhost:8080/service1/hello