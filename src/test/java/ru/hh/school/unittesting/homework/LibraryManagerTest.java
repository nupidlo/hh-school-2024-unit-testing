package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("book1", 5);
    libraryManager.addBook("book2", 3);
  }

  @Test
  void testAddBook() {
    libraryManager.addBook("book4", 2);
    assertEquals(2, libraryManager.getAvailableCopies("book4"));
  }

  @Test
  void testAddExistingBook() {
    libraryManager.addBook("book1", 3);
    assertEquals(8, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void testBorrowBookSuccess() {
    when(userService.isUserActive("user1")).thenReturn(true);

    assertTrue(libraryManager.borrowBook("book1", "user1"));
    assertEquals(4, libraryManager.getAvailableCopies("book1"));

    verify(userService, times(1)).isUserActive("user1");
    verify(notificationService, times(1))
        .notifyUser("user1", "You have borrowed the book: book1");
  }

  @Test
  void testBorrowBookShouldReturnFalseIfUserAccountIsNotActive() {
    when(userService.isUserActive("user1")).thenReturn(false);

    assertFalse(libraryManager.borrowBook("book1", "user1"));
    assertEquals(5, libraryManager.getAvailableCopies("book1"));

    verify(userService, times(1)).isUserActive("user1");
    verify(notificationService, times(1))
        .notifyUser("user1", "Your account is not active.");
  }

  @Test
  void testBorrowBookShouldReturnFalseIfThereAreNoAvailableCopies() {
    when(userService.isUserActive("user1")).thenReturn(true);

    assertFalse(libraryManager.borrowBook("book3", "user1"));
    assertEquals(0, libraryManager.getAvailableCopies("book3"));

    verify(userService, times(1)).isUserActive("user1");
    verify(notificationService, never()).notifyUser(anyString(), anyString());
  }

  @Test
  void testReturnBookSuccess() {
    // Занимаем книгу, чтобы потом ее вернуть
    when(userService.isUserActive("user1")).thenReturn(true);
    assertTrue(libraryManager.borrowBook("book1", "user1"));
    assertEquals(4, libraryManager.getAvailableCopies("book1"));

    assertTrue(libraryManager.returnBook("book1", "user1"));
    assertEquals(5, libraryManager.getAvailableCopies("book1"));

    verify(notificationService, times(1))
        .notifyUser("user1", "You have returned the book: book1");
  }

  @Test
  void getTestReturnBookShouldReturnFalseIfTheBookWasBorrowedByAnotherUser() {
    // Занимаем книгу, чтобы потом ее вернуть
    when(userService.isUserActive("user1")).thenReturn(true);
    assertTrue(libraryManager.borrowBook("book1", "user1"));
    assertEquals(4, libraryManager.getAvailableCopies("book1"));

    assertFalse(libraryManager.returnBook("book1", "user2"));
    assertEquals(4, libraryManager.getAvailableCopies("book1"));

    verify(notificationService, never()).notifyUser(anyString(), eq("You have returned the book: book1"));
  }

  @Test
  void testReturnBookShouldReturnFalseIfTheBookWasNotBorrowed() {
    assertFalse(libraryManager.returnBook("book1", "user1"));
    assertEquals(5, libraryManager.getAvailableCopies("book1"));

    verify(notificationService, never()).notifyUser(anyString(), anyString());
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
      "13, true, true, 7.80",
      "0, true, true, 0.0"
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
