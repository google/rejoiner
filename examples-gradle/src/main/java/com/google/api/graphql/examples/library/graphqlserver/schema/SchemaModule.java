package com.google.api.graphql.examples.library.graphqlserver.schema;

import com.google.inject.AbstractModule;

/** Install all of the schema modules. */
public final class SchemaModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new BookSchemaModule()); // Creates queries and mutations for the Book service
    install(new ShelfSchemaModule()); // Creates queries and mutations for the Shelf service
    install(new LibrarySchemaModule()); // Joins together Shelf and Book services
    install(new SeedLibrarySchemaModule()); // Fills the Shelf and Book services with data
  }
}
