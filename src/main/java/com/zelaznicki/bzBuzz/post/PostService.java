package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.common.PostSort;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    private static String generateSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-|-$", "");
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        return base + "-" + suffix;
    }
    private static String getNormalizedSlug(String slug) {
        return slug == null ? "" : slug.trim().toLowerCase();
    }

    @Transactional
    public Post create(User user, Board board, String title, String text, PostType postType, String url) {
        if (postType == PostType.TEXT && (text == null || text.isBlank())) {
            throw new IllegalArgumentException("Text posts must have a body");
        }
        if (postType == PostType.URL && (url == null || url.isBlank())) {
            throw new IllegalArgumentException("URL posts must have a URL");
        }
        if (text != null && url != null) {
            throw new IllegalArgumentException("A post cannot have both text and a URL");
        }

        String normalizedTitle = title == null ? "" : title.trim();

        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (normalizedTitle.length() > 255) {
            throw new IllegalArgumentException("Title cannot exceed 255 characters");
        }

        String slug = generateSlug(title);

        Post post = Post.builder()
                .creator(user)
                .board(board)
                .title(normalizedTitle)
                .slug(slug)
                .text(text)
                .postType(postType)
                .url(url)
                .voteScore(0)
                .status(Status.ENABLED)
                .build();

        return postRepository.save(post);

    }

    public Post findBySlug(String slug) {
        String normalizedSlug = getNormalizedSlug(slug);

        return postRepository.findBySlug(normalizedSlug)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }



    public List<Post> findByBoard(Board board, PostSort postSort) {
        return switch (postSort) {
            case NEW -> postRepository.findAllByBoardAndStatusOrderByCreatedAtDesc(board, Status.ENABLED);
            case UPDATED -> postRepository.findAllByBoardAndStatusOrderByUpdatedAtDesc(board, Status.ENABLED);
            case TOP -> postRepository.findAllByBoardAndStatusOrderByVoteScoreDesc(board, Status.ENABLED);
        };
    }

    public List<Post> findByUser(User user, PostSort postSort) {
        return switch (postSort) {
            case NEW -> postRepository.findAllByUserAndStatusOrderByCreatedAtDesc(user, Status.ENABLED);
            case UPDATED -> postRepository.findAllByUserAndStatusOrderByUpdatedAtDesc(user, Status.ENABLED);
            case TOP -> postRepository.findAllByUserAndStatusOrderByVoteScoreDesc(user, Status.ENABLED);
        };
    }

    @Transactional
    public Post updatePost(String slug, User user, String title, String text, String url) {
        String normalizedSlug = getNormalizedSlug(slug);

        if (normalizedSlug.isEmpty()) {
            throw new IllegalArgumentException("Slug cannot be empty");
        }
        Post post = postRepository.findBySlug(normalizedSlug).orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getCreator() == null || !post.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to perform this action");
        }

        String normalizedTitle = title == null ? "" : title.trim();

        if (!normalizedTitle.isEmpty()) {
            post.setTitle(normalizedTitle);
        }

        if (text != null && !text.isBlank()) {
            post.setText(text);
        }

        if (url != null && !url.isBlank()) {
            post.setUrl(url);
        }

        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(String slug, User user) {
        String normalizedSlug = getNormalizedSlug(slug);

        if (normalizedSlug.isEmpty()) {
            throw new IllegalArgumentException("Slug cannot be empty");
        }

        Post post = postRepository.findBySlug(normalizedSlug).orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getCreator() == null || !post.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to perform this action");
        }

        post.setStatus(Status.DISABLED);
        postRepository.save(post);
    }
}
