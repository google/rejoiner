// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google;

import com.google.api.graphql.rejoiner.SchemaProviderModule;
import com.google.api.graphql.schema.cloud.container.ContainerClientModule;
import com.google.api.graphql.schema.cloud.container.ContainerSchemaModule;
import com.google.api.graphql.schema.firestore.FirestoreClientModule;
import com.google.api.graphql.schema.firestore.FirestoreSchemaModule;
import com.google.api.graphql.schema.protobuf.TimestampSchemaModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.PathResource;

import java.io.File;
import java.util.EnumSet;
import java.util.logging.Logger;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.REQUEST;
import static org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS;

public class GraphQlServer {

  private static final int HTTP_PORT = 8080;
  private static final Logger logger = Logger.getLogger(GraphQlServer.class.getName());

  public static void main(String[] args) throws Exception {
    Server server = new Server(HTTP_PORT);

    ServletContextHandler context = new ServletContextHandler(server, "/", SESSIONS);

    context.addEventListener(
        new GuiceServletContextListener() {
          @Override
          protected Injector getInjector() {
            return Guice.createInjector(
                new ServletModule() {
                  @Override
                  protected void configureServlets() {
                    serve("/graphql").with(GraphQlServlet.class);
                  }
                },
                new SchemaProviderModule(),
                new FirestoreClientModule(),
                new ContainerClientModule(),
                new FirestoreSchemaModule(),
                new ContainerSchemaModule(),
                new TimestampSchemaModule());
          }
        });

    context.addFilter(GuiceFilter.class, "/*", EnumSet.of(REQUEST, ASYNC));

    context.setBaseResource(
        new PathResource(new File("./src/main/resources").toPath().toRealPath()));
    context.addServlet(DefaultServlet.class, "/");
    server.start();
    logger.info("Server running on port " + HTTP_PORT);
    server.join();
  }
}
