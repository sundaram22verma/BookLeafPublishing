package com.bookleaf.support.repository;

import com.bookleaf.support.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByAuthorIdOrderByPublicationDateDescTitleAsc(Long authorId);

    /** Ownership-safe lookup: returns empty if the book belongs to someone else. */
    Optional<Book> findByIdAndAuthorId(Long id, Long authorId);
}
