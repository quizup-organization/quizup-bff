package io.github.quizup.bff.infrastructure.in.api;

import io.github.quizup.bff.infrastructure.in.api.mapper.PageMapper;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Ressource {@code /api/topics} — lecture et recherche de sujets via le bus de requêtes distribué.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final QueryGateway queryGateway;

    public TopicController(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<Topic>>> search(
            @RequestBody(required = false) SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return queryGateway
                .query(
                        new TopicQuery.TopicSearchQuery(criteria.filters(), criteria.sorts(), criteria.page()),
                        QueryResponseTypes.pageResultOf(Topic.class)
                )
                .thenApply(PageMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{topicId}")
    public CompletableFuture<ResponseEntity<Topic>> getById(@PathVariable String topicId) {
        return queryGateway
                .query(new TopicQuery.GetTopicByIdQuery(topicId), QueryResponseTypes.instanceOf(Topic.class))
                .thenApply(ResponseEntity::ok);
    }
}
