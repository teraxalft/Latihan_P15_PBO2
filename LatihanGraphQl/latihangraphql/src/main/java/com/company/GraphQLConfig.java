package com.company;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class GraphQLConfig {
    public static GraphQL init() throws IOException {
       InputStream schemaStream = GraphQLConfig.class.getClassLoader().getResourceAsStream("schema.graphqls");
       
       if (schemaStream == null) {
           throw new RuntimeException("schema.graphqls not found in classpath.");
       }
       
       String schema = new String(Objects.requireNonNull(schemaStream).readAllBytes());

       TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schema);
       RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
           .type("Query", builder -> builder
               .dataFetcher("allProducts", env -> ProductRepository.findAll())
               .dataFetcher("productById", env -> {
                   Long id = env.getArgument("id");
                   return ProductRepository.findById(id);
               })
           )
           .type("Mutation", builder -> builder
               .dataFetcher("addProduct", env -> ProductRepository.add(
                   env.getArgument("name"),
                   ((Number) env.getArgument("price")).doubleValue(),
                   env.getArgument("category")
               ))
               .dataFetcher("deleteProduct", env -> {
                    String idStr = env.getArgument("id");
                    Long id = Long.parseLong(idStr);
                    return ProductRepository.delete(id);
                })
                .dataFetcher("updateProduct", env -> {
                    String idStr = env.getArgument("id");
                    Long id = Long.parseLong(idStr);
                    String name = env.getArgument("name");
                    Double price = ((Number) env.getArgument("price")).doubleValue();
                    String category = env.getArgument("category");
                    return ProductRepository.update(id, name, price, category);
                })
           )
           .build();
       GraphQLSchema schemaFinal = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
       return GraphQL.newGraphQL(schemaFinal).build();
   }  
}
