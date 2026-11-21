package com.collegeeventhub.resource.repository;

import com.collegeeventhub.resource.entity.ResourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<ResourceEntity, Long> {

    @Query(
            value = """
                    select distinct resource from ResourceEntity resource
                    left join resource.tags tag
                    where resource.active = true
                      and resource.organization.active = true
                      and (:category is null
                           or lower(resource.category) = :category)
                      and (:tag is null or lower(tag) = :tag)
                      and (:query is null
                           or lower(resource.title) like concat('%', :query, '%')
                           or lower(resource.description)
                                like concat('%', :query, '%')
                           or lower(resource.category)
                                like concat('%', :query, '%'))
                    """,
            countQuery = """
                    select count(distinct resource.id)
                    from ResourceEntity resource
                    left join resource.tags tag
                    where resource.active = true
                      and resource.organization.active = true
                      and (:category is null
                           or lower(resource.category) = :category)
                      and (:tag is null or lower(tag) = :tag)
                      and (:query is null
                           or lower(resource.title) like concat('%', :query, '%')
                           or lower(resource.description)
                                like concat('%', :query, '%')
                           or lower(resource.category)
                                like concat('%', :query, '%'))
                    """
    )
    Page<ResourceEntity> findActiveCatalog(
            @Param("query") String query,
            @Param("category") String category,
            @Param("tag") String tag,
            Pageable pageable
    );

    Optional<ResourceEntity> findByIdAndActiveTrue(Long id);

    Page<ResourceEntity> findAllByOrganizationId(
            Long organizationId,
            Pageable pageable
    );

    Page<ResourceEntity> findAllByOrganizationIdIn(
            Collection<Long> organizationIds,
            Pageable pageable
    );
}
