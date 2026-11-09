package com.library.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
@Configuration
public class TomcatConfig {
 // NIO2 uses Windows asynchronous I/O, avoiding the JDK selector's Unix-domain wakeup pipe.
 @Bean TomcatServletWebServerFactory webServer(){var factory=new TomcatServletWebServerFactory();factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");return factory;}
}
