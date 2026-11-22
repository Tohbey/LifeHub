package com.lifehub.notes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lifehub.notes.model.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    /** A bookmark of this workspace that this user may see: shared, or their own. */
    @Query("""
            select x from Bookmark x
            where x.id = :id and x.workspaceId = :workspaceId
              and (x.visibility = com.lifehub.notes.model.entity.Visibility.SHARED or x.createdByUserId = :userId)
            """)
    Optional<Bookmark> findVisible(UUID id, UUID workspaceId, UUID userId);

    /** The bookmarks this user may see, optionally only those matching {@code q} or carrying {@code tag}. */
    @Query(value = """
            select x from Bookmark x
            where x.workspaceId = :workspaceId
              and (x.visibility = com.lifehub.notes.model.entity.Visibility.SHARED or x.createdByUserId = :userId)
              and (:q is null or lower(x.title) like lower(concat('%', :q, '%')) or lower(x.url) like lower(concat('%', :q, '%')) or lower(x.description) like lower(concat('%', :q, '%')))
              and (:tag is null or :tag member of x.tags)
            order by x.createdAt desc
            """, countQuery = """
            select count(x) from Bookmark x
            where x.workspaceId = :workspaceId
              and (x.visibility = com.lifehub.notes.model.entity.Visibility.SHARED or x.createdByUserId = :userId)
              and (:q is null or lower(x.title) like lower(concat('%', :q, '%')) or lower(x.url) like lower(concat('%', :q, '%')) or lower(x.description) like lower(concat('%', :q, '%')))
              and (:tag is null or :tag member of x.tags)
            """)
    Page<Bookmark> search(UUID workspaceId, UUID userId, String q, String tag, Pageable pageable);

    /** The tags on the bookmarks this user may see, with how many carry each. */
    @Query("""
            select new com.lifehub.notes.repository.TagCount(t, count(x)) from Bookmark x join x.tags t
            where x.workspaceId = :workspaceId
              and (x.visibility = com.lifehub.notes.model.entity.Visibility.SHARED or x.createdByUserId = :userId)
            group by t
            """)
    List<TagCount> countTags(UUID workspaceId, UUID userId);
}
