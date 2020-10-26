package de.scrum_master.stackoverflow;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class TimerAgent {
  public static void main(String[] args) throws IOException {
    Instrumentation instrumentation = ByteBuddyAgent.install();

    ElementMatcher.Junction<MethodDescription> executeMethodDecription = isMethod()
      .and(named("execute"))
      .and(not(isAbstract()))
      .and(takesArguments(3))
      .and(takesArgument(0, named("org.apache.http.client.methods.HttpUriRequest")))
      .and(takesArgument(1, named("org.apache.http.client.ResponseHandler")))
      .and(takesArgument(2, named("org.apache.http.protocol.HttpContext")));

    new AgentBuilder.Default()
      .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
      .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
      .type(isSubTypeOf(HttpClient.class).and(declaresMethod(executeMethodDecription)))
      .transform((builder, type, classLoader, module) ->
        builder.method(executeMethodDecription).intercept(MethodDelegation.to(TimingInterceptor.class))
      )
      .installOn(instrumentation);

    HttpClient client = new DefaultHttpClient();
    HttpUriRequest httpUriRequest = new HttpGet("http://www.google.com");
    client.execute(
      httpUriRequest,
      response -> {
        System.out.println("Handling response: " + response);
        return response;
      }
    );
  }

}
