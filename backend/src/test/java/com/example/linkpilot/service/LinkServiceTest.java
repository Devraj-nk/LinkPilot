package com.example.linkpilot.service;

import com.example.linkpilot.dto.LinkRequest;
import com.example.linkpilot.exception.DuplicateResourceException;
import com.example.linkpilot.exception.ForbiddenException;
import com.example.linkpilot.exception.GoneException;
import com.example.linkpilot.exception.ResourceNotFoundException;
import com.example.linkpilot.model.Campaign;
import com.example.linkpilot.model.Domain;
import com.example.linkpilot.model.DomainVerificationStatus;
import com.example.linkpilot.model.Link;
import com.example.linkpilot.model.LinkStatus;
import com.example.linkpilot.repository.CampaignRepository;
import com.example.linkpilot.repository.DomainRepository;
import com.example.linkpilot.repository.LinkRepository;
import com.example.linkpilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private DomainRepository domainRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShortCodeGenerator shortCodeGenerator;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private LinkService linkService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        linkService = new LinkService(linkRepository, campaignRepository, domainRepository, userRepository, shortCodeGenerator, redisTemplate);
        lenient().when(linkRepository.save(any(Link.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ---- create() ----

    @Test
    void create_generatesShortCodeWhenNoCustomCodeGiven() {
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        LinkRequest request = new LinkRequest("https://example.com", null, null, null, null, null);

        Link link = linkService.create(userId, request);

        assertThat(link.getShortCode()).isEqualTo("abc1234");
        verify(shortCodeGenerator).generate();
    }

    @Test
    void create_honorsAvailableCustomCode() {
        when(linkRepository.existsByShortCode("mycode")).thenReturn(false);
        LinkRequest request = new LinkRequest("https://example.com", null, null, null, "mycode", null);

        Link link = linkService.create(userId, request);

        assertThat(link.getShortCode()).isEqualTo("mycode");
        verify(shortCodeGenerator, never()).generate();
    }

    @Test
    void create_rejectsAlreadyTakenCustomCode() {
        when(linkRepository.existsByShortCode("taken")).thenReturn(true);
        LinkRequest request = new LinkRequest("https://example.com", null, null, null, "taken", null);

        assertThrows(DuplicateResourceException.class, () -> linkService.create(userId, request));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void create_rejectsCampaignTheCallerDoesNotOwn() {
        UUID campaignId = UUID.randomUUID();
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(campaignRepository.findByIdAndUserId(campaignId, userId)).thenReturn(Optional.empty());
        LinkRequest request = new LinkRequest("https://example.com", null, campaignId, null, null, null);

        assertThrows(ResourceNotFoundException.class, () -> linkService.create(userId, request));
    }

    @Test
    void create_rejectsDomainTheCallerDoesNotOwn() {
        UUID domainId = UUID.randomUUID();
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(domainRepository.findByIdAndUserId(domainId, userId)).thenReturn(Optional.empty());
        LinkRequest request = new LinkRequest("https://example.com", null, null, domainId, null, null);

        assertThrows(ResourceNotFoundException.class, () -> linkService.create(userId, request));
    }

    @Test
    void create_rejectsUnverifiedDomain() {
        UUID domainId = UUID.randomUUID();
        Domain domain = new Domain();
        domain.setId(domainId);
        domain.setVerificationStatus(DomainVerificationStatus.PENDING);
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(domainRepository.findByIdAndUserId(domainId, userId)).thenReturn(Optional.of(domain));
        LinkRequest request = new LinkRequest("https://example.com", null, null, domainId, null, null);

        assertThrows(ForbiddenException.class, () -> linkService.create(userId, request));
    }

    @Test
    void create_acceptsVerifiedDomain() {
        UUID domainId = UUID.randomUUID();
        Domain domain = new Domain();
        domain.setId(domainId);
        domain.setDomain("go.example.com");
        domain.setVerificationStatus(DomainVerificationStatus.VERIFIED);
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(domainRepository.findByIdAndUserId(domainId, userId)).thenReturn(Optional.of(domain));
        LinkRequest request = new LinkRequest("https://example.com", null, null, domainId, null, null);

        Link link = linkService.create(userId, request);

        assertThat(link.getDomain()).isEqualTo(domain);
    }

    // ---- getForUser() ----

    @Test
    void getForUser_throwsNotFoundForNonOwner() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findByIdAndUserId(linkId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> linkService.getForUser(userId, linkId));
    }

    // ---- resolve() ----

    @Test
    void resolve_incrementsClickCountForActiveLink() {
        Link link = activeLink();
        when(linkRepository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        Link resolved = linkService.resolve("abc1234", null);

        assertThat(resolved.getClickCount()).isEqualTo(1);
    }

    @Test
    void resolve_throwsGoneForDisabledLink() {
        Link link = activeLink();
        link.setStatus(LinkStatus.DISABLED);
        when(linkRepository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        assertThrows(GoneException.class, () -> linkService.resolve("abc1234", null));
    }

    @Test
    void resolve_throwsGoneForExpiredLink() {
        Link link = activeLink();
        link.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(linkRepository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        assertThrows(GoneException.class, () -> linkService.resolve("abc1234", null));
    }

    @Test
    void resolve_throwsNotFoundWhenHostDoesNotMatchLinksDomain() {
        Link link = activeLink();
        Domain domain = new Domain();
        domain.setDomain("go.example.com");
        link.setDomain(domain);
        when(linkRepository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        assertThrows(ResourceNotFoundException.class, () -> linkService.resolve("abc1234", "other-host.com"));
    }

    @Test
    void resolve_succeedsWhenHostMatchesLinksDomain() {
        Link link = activeLink();
        Domain domain = new Domain();
        domain.setDomain("go.example.com");
        link.setDomain(domain);
        when(linkRepository.findByShortCode("abc1234")).thenReturn(Optional.of(link));

        Link resolved = linkService.resolve("abc1234", "go.example.com:443");

        assertThat(resolved.getClickCount()).isEqualTo(1);
    }

    @Test
    void resolve_throwsNotFoundForUnknownShortCode() {
        when(linkRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> linkService.resolve("missing", null));
    }

    private Link activeLink() {
        Link link = new Link();
        link.setId(UUID.randomUUID());
        link.setShortCode("abc1234");
        link.setOriginalUrl("https://example.com");
        link.setStatus(LinkStatus.ACTIVE);
        link.setClickCount(0);
        return link;
    }
}
