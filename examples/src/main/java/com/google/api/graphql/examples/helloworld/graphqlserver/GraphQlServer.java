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

package com.google.api.graphql.examples.helloworld.graphqlserver;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import java.util.logging.Logger;

public class GraphQlServer {

  private static final int HTTP_PORT = 8080;
  private static final Logger logger = Logger.getLogger(GraphQlServer.class.getName());

  public static void main(String[] args) throws Exception {
    // Embedded Jetty server
    Server server = new Server(HTTP_PORT);
    ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setWelcomeFiles(new String[] {"index.html"});
    resourceHandler.setDirectoriesListed(true);
    // resource base is relative to the WORKSPACE file
    resourceHandler.setResourceBase("./src/main/resources");
    HandlerList handlerList = new HandlerList();
    handlerList.setHandlers(
        new Handler[] {resourceHandler, new GraphQlHandler(), new DefaultHandler()});
    server.setHandler(handlerList);
    server.start();
    logger.info("Server running on port " + HTTP_PORT);
    server.join();
  }
}
