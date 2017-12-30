package com.google.api.graphql.examples.library.bookbackend;

import com.google.common.collect.ImmutableList;
import com.google.example.library.book.v1.Book;
import com.google.example.library.book.v1.BookServiceGrpc;
import com.google.example.library.book.v1.CreateBookRequest;
import com.google.example.library.book.v1.DeleteBookRequest;
import com.google.example.library.book.v1.GetBookRequest;
import com.google.example.library.book.v1.ListBooksRequest;
import com.google.example.library.book.v1.ListBooksResponse;
import com.google.example.library.book.v1.UpdateBookRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.util.Base64;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.GuardedBy;

public class BookService extends BookServiceGrpc.BookServiceImplBase {

  private static final long DEFAULT_PAGE_SIZE = 4;
  private final AtomicInteger bookIdCounter = new AtomicInteger(0);

  @GuardedBy("this")
  private final TreeMap<String, Book> booksById = new TreeMap<>();

  @Override
  public synchronized void createBook(
      CreateBookRequest request, StreamObserver<Book> responseObserver) {
    String id = bookIdCounter.getAndIncrement() + "";
    booksById.put(id, request.getBook().toBuilder().setId(id).build());
    responseObserver.onNext(booksById.get(id));
    responseObserver.onCompleted();
  }

  @Override
  public synchronized void getBook(GetBookRequest request, StreamObserver<Book> responseObserver) {
    responseObserver.onNext(booksById.get(request.getId()));
    responseObserver.onCompleted();
  }

  @Override
  public synchronized void listBooks(
      ListBooksRequest request, StreamObserver<ListBooksResponse> responseObserver) {
    NavigableMap<String, Book> cursor = booksById;

    // Resume iteration from the page token.
    if (!request.getPageToken().isEmpty()) {
      String pageToken = decodePageToken(request.getPageToken());
      cursor = cursor.tailMap(pageToken, false);
    }

    ImmutableList<Book> books =
        cursor
            .values()
            .stream()
            .limit(request.getPageSize() > 0 ? request.getPageSize() : DEFAULT_PAGE_SIZE)
            .collect(ImmutableList.toImmutableList());

    // Return one page of results.
    ListBooksResponse.Builder responseBuilder = ListBooksResponse.newBuilder().addAllBooks(books);
    // Set next page token to resume iteration in the next request.
    if (cursor.values().size() > books.size()) {
      String nextPageToken = encodePageToken(books.get(books.size() - 1).getId());
      responseBuilder.setNextPageToken(nextPageToken);
    }

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public synchronized void streamBooks(
      ListBooksRequest request, StreamObserver<ListBooksResponse> responseObserver) {

    NavigableMap<String, Book> cursor = booksById;

    // Resume iteration from the page token.
    if (!request.getPageToken().isEmpty()) {
      String pageToken = decodePageToken(request.getPageToken());
      cursor = cursor.tailMap(pageToken, false);
    }

    cursor
        .values()
        .stream()
        .limit(request.getPageSize() > 0 ? request.getPageSize() : DEFAULT_PAGE_SIZE)
        .forEach(
            book ->
                responseObserver.onNext(
                    ListBooksResponse.newBuilder()
                        .addBooks(book)
                        .setNextPageToken(encodePageToken(book.getId()))
                        .build()));
    responseObserver.onCompleted();
  }

  @Override
  public synchronized void deleteBook(
      DeleteBookRequest request, StreamObserver<Empty> responseObserver) {
    if (booksById.remove(request.getId()) == null) {
      throw new RuntimeException(String.format("Book with id=%s not found", request.getId()));
    }
  }

  @Override
  public synchronized void updateBook(
      UpdateBookRequest request, StreamObserver<Book> responseObserver) {
    if (booksById.replace(request.getId(), request.getBook()) == null) {
      throw new RuntimeException(String.format("Book with id=%s not found", request.getId()));
    }
  }

  private static String encodePageToken(String token) {
    return new String(Base64.getEncoder().encode(token.getBytes()));
  }

  private static String decodePageToken(String encodedToken) {
    return new String(Base64.getDecoder().decode(encodedToken.getBytes()));
  }
}
