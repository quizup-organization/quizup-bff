package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.FilterOperator;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.FilterRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.PageRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.query.PresenceQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/presence} — présence joueur (read-model éphémère, pilote le cycle de vie STOMP).
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final QueryGateway queryGateway;

    public PresenceController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    /** Présence d'un joueur ; un inconnu est retourné {@code OFFLINE}. */
    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<PlayerPresence>> get(@PathVariable String userId) {
        List<FilterCriteria> filters = new ArrayList<>();
        filters.add(new FilterRequest("userId", FilterOperator.EQUALS, userId, null, null));
        return queryGateway
                .query(
                        new PresenceQuery.PresenceSearchQuery(filters, List.of(), new PageRequest(0, 1)),
                        QueryResponseTypes.pageResultOf(PlayerPresence.class)
                )
                .thenApply(page -> page.content().stream().findFirst()
                        .orElseGet(() -> PlayerPresence.builder().userId(userId).status(PresenceStatus.OFFLINE).build()))
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<PlayerPresence>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return queryGateway
                .query(
                        new PresenceQuery.PresenceSearchQuery(criteria.filters(), criteria.sorts(), criteria.page()),
                        QueryResponseTypes.pageResultOf(PlayerPresence.class)
                )
                .thenApply(PageMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}
