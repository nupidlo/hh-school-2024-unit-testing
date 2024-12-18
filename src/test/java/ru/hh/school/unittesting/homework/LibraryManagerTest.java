package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {

  // приватные поля, к которым будем получать доступ
  private Field bookInventory;
  private Field borrowedBooks;

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  public LibraryManagerTest() throws NoSuchFieldException {
  }

  @BeforeEach
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    // получение доступа к приватным полям libraryManager и установка дефолтных значений для тестов
    bookInventory = LibraryManager.class.getDeclaredField("bookInventory");
    bookInventory.setAccessible(true);
    HashMap<String, Integer> bookInventoryToSet = new HashMap<>(Map.of("book1", 5, "book2", 3));
    bookInventory.set(libraryManager, bookInventoryToSet);

    borrowedBooks = LibraryManager.class.getDeclaredField("borrowedBooks");
    borrowedBooks.setAccessible(true);
    HashMap<String, String> borrowedBooksToSet = new HashMap<>(Map.of("book1", "user1"));
    borrowedBooks.set(libraryManager, borrowedBooksToSet);
  }

  @Test
  void testAddBook() {
    libraryManager.addBook("book4", 2);
    assertEquals(2, libraryManager.getAvailableCopies("book4"));
  }

  /* Параметризованный тест для borrowBook()
  * 1 - успешное взятие книги
  * 2 - ошибка, пользователь с неактивным аккаунтом
  * 3 - ошибка, книги нет в наличии */
  @ParameterizedTest
  @CsvSource({
      "book1, user1, true, true, 4, You have borrowed the book: book1",
      "book1, user2, false, false, 5, Your account is not active.",
      "book3, user1, true, false, 0, ",
  })
  void testBorrowBook(
      String bookId,
      String userId,
      boolean userServiceResponse,
      boolean expectedResult,
      int expectedQuantity,
      String expectedMsg
  ) throws IllegalAccessException {
    when(userService.isUserActive(userId)).thenReturn(userServiceResponse);

    assertEquals(expectedResult, libraryManager.borrowBook(bookId, userId));

    Map<String, Integer> availableBooks = (HashMap<String, Integer>) bookInventory.get(libraryManager);
    assertEquals(expectedQuantity, availableBooks.getOrDefault(bookId, 0));

    if (expectedResult) {
      Map<String, String> borrowedBooksMap = (HashMap<String, String>) borrowedBooks.get(libraryManager);
      assertEquals(userId, borrowedBooksMap.getOrDefault(bookId, null));
    }

    verify(userService, times(1)).isUserActive(userId);

    if (expectedMsg != null) {
      verify(notificationService, times(1)).notifyUser(userId, expectedMsg);
    } else {
      verify(notificationService, never()).notifyUser(userId, null);
    }
  }

  /* Параметризованный тест для returnBook()
   * 1 - успешное возвращение книги
   * 2 - ошибка, пользователь пытается вернуть не свою книгу
   * 3 - ошибка, книги нет в наличии */
  @ParameterizedTest
  @CsvSource({
      "book1, user1, true, 6, You have returned the book: book1",
      "book1, user2, false, 5, ",
      "book3, user1, false, 0, "
  })
  void testReturnBook(
      String bookId,
      String userId,
      boolean expectedResult,
      int expectedQuantity,
      String expectedMsg
  ) throws IllegalAccessException {
    assertEquals(expectedResult, libraryManager.returnBook(bookId, userId));

    Map<String, Integer> availableBooks = (HashMap<String, Integer>) bookInventory.get(libraryManager);
    assertEquals(expectedQuantity, availableBooks.getOrDefault(bookId, 0));

    Map<String, String> borrowedBooksMap = (HashMap<String, String>) borrowedBooks.get(libraryManager);
    if (expectedResult) {
      assertNull(borrowedBooksMap.getOrDefault(bookId, null));
    } else {
      assertNotEquals(userId, borrowedBooksMap.getOrDefault(bookId, null));
    }

    if (expectedMsg != null) {
      verify(notificationService, times(1)).notifyUser(userId, expectedMsg);
    } else {
      verify(notificationService, never()).notifyUser(userId, null);
    }
  }

  @Test
  void calculateDynamicLateFeeShouldThrowExceptionWhenOverdueDaysIsNegative() {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, true, true)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "10, false, false, 5.00",
      "13, false, false, 6.50",
      "13, true, false, 9.75",
      "13, false, true, 5.20",
      "13, true, true, 7.80"
  })
  void testCalculateDynamicLateFee(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double expectedResult
  ) {
    assertEquals(expectedResult, libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember));
  }
}
