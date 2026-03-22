package com.zelaznicki.bzBuzz.post;

import com.zelaznicki.bzBuzz.board.Board;
import com.zelaznicki.bzBuzz.common.PostSort;
import com.zelaznicki.bzBuzz.common.Status;
import com.zelaznicki.bzBuzz.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostVoteRepository postVoteRepository;

    private static final int UPVOTE = 1;
    private static final int DOWNVOTE = -1;

    /**
     * Create a URL-friendly slug from a post title with a short random suffix.
     *
     * The title is normalized (lowercased, non-alphanumeric/space/hyphen characters removed,
     * consecutive spaces/hyphens collapsed to a single '-', and leading/trailing '-' trimmed),
     * then a 6-character random UUID substring is appended, separated by a hyphen.
     *
     * @param title the original post title
     * @return the generated slug consisting of the normalized title base and a 6-character random suffix
     */
    private static String generateSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-|-$", "");
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        return base + "-" + suffix;
    }
    /**
     * Normalize a slug by trimming leading/trailing whitespace and converting to lowercase.
     *
     * @param slug the slug to normalize; may be {@code null}
     * @return the normalized slug, or an empty string if {@code slug} is {@code null}
     */
    private static String getNormalizedSlug(String slug) {
        return slug == null ? "" : slug.trim().toLowerCase();
    }

    private void validatePostData(String title, String text, PostType postType, String url) {

        if (postType == PostType.TEXT && text == null) {
            throw new IllegalArgumentException("Text posts must have a body");
        }
        if (postType == PostType.URL && url == null) {
            throw new IllegalArgumentException("URL posts must have a URL");
        }

        if (url != null && (url.startsWith("javascript:") || url.startsWith("data:"))) {
            throw new IllegalArgumentException("Invalid URL");
        }

        if (text != null && url != null) {
            throw new IllegalArgumentException("A post cannot have both text and a URL");
        }
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Title cannot exceed 255 characters");
        }

    }

    /**
     * Create and persist a new post with normalized inputs, validated constraints, and a generated slug.
     *
     * The method trims the title and treats blank `text` or `url` as absent; it enforces that:
     * - A TEXT post must include non-blank `text`.
     * - A URL post must include a non-blank `url`.
     * - A post cannot contain both `text` and `url`.
     * The saved post is initialized with a vote score of 0 and status ENABLED.
     *
     * @param user the creator of the post
     * @param board the board where the post will be published
     * @param title the post title; it is trimmed and must contain 1 to 255 characters
     * @param text the post body; blank values are treated as absent
     * @param postType the post type which dictates required content (e.g., TEXT or URL)
     * @param url the post URL; blank values are treated as absent
     * @return the persisted Post with a generated slug, voteScore = 0, and status = ENABLED
     * @throws IllegalArgumentException if validation fails (missing required content for the given postType, both text and url provided, empty title, or title longer than 255 characters)
     */
    @Transactional
    public Post create(User user, Board board, String title, String text, PostType postType, String url) {
        String normalizedText = (text == null || text.isBlank()) ? null : text;
        String normalizedUrl = (url == null || url.isBlank()) ? null : url;
        String normalizedTitle = title == null ? "" : title.trim();

        try {
            validatePostData(normalizedTitle, normalizedText, postType, normalizedUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        String slug = generateSlug(normalizedTitle);

        Post post = Post.builder()
                .creator(user)
                .board(board)
                .title(normalizedTitle)
                .slug(slug)
                .text(normalizedText)
                .postType(postType)
                .url(normalizedUrl)
                .voteScore(0)
                .status(Status.ENABLED)
                .build();

        return postRepository.save(post);

    }

    /**
     * Finds a post by slug after normalizing the input slug.
     *
     * @param slug the slug to look up; will be trimmed and lowercased before query
     * @return the matched Post
     * @throws IllegalArgumentException if no post exists for the normalized slug
     */
    public Post findBySlug(String slug) {
        String normalizedSlug = getNormalizedSlug(slug);

        return postRepository.findBySlugAndStatus(normalizedSlug, Status.ENABLED)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }



    /**
     * Retrieve enabled posts for the given board ordered according to the specified sort.
     *
     * @param postSort the ordering to apply: NEW (creation time desc), UPDATED (update time desc), or TOP (vote score desc)
     * @return the enabled posts for the board ordered according to {@code postSort}
     */
    public List<Post> findByBoard(Board board, PostSort postSort) {
        return switch (postSort) {
            case NEW -> postRepository.findAllByBoardAndStatusOrderByCreatedAtDesc(board, Status.ENABLED);
            case UPDATED -> postRepository.findAllByBoardAndStatusOrderByUpdatedAtDesc(board, Status.ENABLED);
            case TOP -> postRepository.findAllByBoardAndStatusOrderByVoteScoreDesc(board, Status.ENABLED);
        };
    }

    public Post findByBoardAndSlug(Board board, String slug) {
        String normalizedSlug = getNormalizedSlug(slug);
        Optional<Post> foundPost = postRepository.findBySlug(normalizedSlug);

        if (foundPost.isPresent()) {
            Post post = foundPost.get();

            if (post.getBoard().equals(board)) {
                return post;
            } else {
                throw new IllegalArgumentException("Post is not part of this board");
            }
        }
        return null;
    }

    /**
     * Retrieve enabled posts created by a user, ordered according to the specified sort.
     *
     * @param user     the creator whose posts to retrieve
     * @param postSort the sort order to apply: NEW (created time desc), UPDATED (updated time desc), or TOP (vote score desc)
     * @return a list of enabled posts created by the given user ordered per {@code postSort}
     */
    public List<Post> findByUser(User user, PostSort postSort) {
        return switch (postSort) {
            case NEW -> postRepository.findAllByCreatorAndStatusOrderByCreatedAtDesc(user, Status.ENABLED);
            case UPDATED -> postRepository.findAllByCreatorAndStatusOrderByUpdatedAtDesc(user, Status.ENABLED);
            case TOP -> postRepository.findAllByCreatorAndStatusOrderByVoteScoreDesc(user, Status.ENABLED);
        };
    }

    /**
     * Updates an existing post identified by the given slug, applying any provided title, text, or URL changes, and returns the saved post.
     *
     * The method normalizes the slug, verifies the post exists, and confirms the requesting user is the post's creator. If a non-empty title is provided it replaces the post's title; if non-blank text or URL values are provided they replace the post's text or URL respectively. The updated post is persisted and returned.
     *
     * @param slug  the post slug to identify which post to update
     * @param user  the user attempting the update (must be the post's creator)
     * @param title the new title to set (trimmed and applied only if not empty)
     * @param text  the new text body to set (applied only if not blank)
     * @param url   the new URL to set (applied only if not blank)
     * @return the persisted updated Post
     * @throws IllegalArgumentException if the normalized slug is empty, the post is not found, or the user is not authorized to update the post
     */
    @Transactional
    public Post updatePost(String slug, User user, String title, String text, PostType postType, String url) {
        String normalizedSlug = getNormalizedSlug(slug);

        if (normalizedSlug.isEmpty()) {
            throw new IllegalArgumentException("Slug cannot be empty");
        }
        Post post = postRepository.findBySlug(normalizedSlug).orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getCreator() == null || !post.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to perform this action");
        }

        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedText = (text == null || text.isBlank()) ? null : text;
        String normalizedUrl = (url == null || url.isBlank()) ? null : url;


        if (!normalizedTitle.isEmpty()) {
            post.setTitle(normalizedTitle);
        }

        if (text != null && !text.isBlank()) {
            post.setText(normalizedText);
        }
        if (postType != null) {
            post.setPostType(postType);
        }

        if (url != null && !url.isBlank()) {
            post.setUrl(normalizedUrl);
        }
        try {
            validatePostData(post.getTitle(), post.getText(), post.getPostType(), post.getUrl());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        return postRepository.save(post);
    }


    /**
     * Apply a user's vote to a post, updating the stored vote record and the post's vote score.
     *
     * @param post the post being voted on
     * @param user the user casting the vote
     * @param voteType either {@code UPVOTE} (1) or {@code DOWNVOTE} (-1) to indicate the vote direction
     * @return a {@code VoteResponse} containing the post's updated vote score and an action string
     *         ("upvoted", "downvoted", or "unvoted")
     * @throws IllegalArgumentException if the post cannot be found or if {@code voteType} is invalid
     */
    @Transactional
    public VoteResponse vote(Post post, User user, int voteType) {

        Post lockedPost = postRepository.findByIdForUpdate(post.getId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (voteType != UPVOTE && voteType != DOWNVOTE) {
            throw new IllegalArgumentException("Invalid vote");
        }

        int delta = 0;
        String action;
        Optional<PostVote> existing = postVoteRepository.findByPostAndUser(lockedPost, user);
        if (existing.isPresent()) {
            PostVote currentVote = existing.get();

            if (currentVote.getVoteType() == voteType) {
                // un-vote
                action = "unvoted";
                postVoteRepository.delete(currentVote);
                delta = -voteType;
            } else {
                // switch vote
                action = voteType == UPVOTE ? "upvoted" : "downvoted";
                currentVote.setVoteType(voteType);
                postVoteRepository.save(currentVote);
                delta = voteType * 2;
            }
        } else {
            // new vote
            action = voteType == UPVOTE ? "upvoted" : "downvoted";
            PostVote postVote = PostVote.builder()
                    .post(lockedPost)
                    .user(user)
                    .voteType(voteType)
                    .build();
            postVoteRepository.save(postVote);
            delta = voteType;
        }

        postRepository.adjustVoteScore(lockedPost.getId(), delta);
        return new VoteResponse(lockedPost.getVoteScore() + delta, action);
    }

    /**
     * Maps enabled posts in the given board to the vote type cast by the specified user.
     *
     * @param board the board whose enabled posts are considered
     * @param user  the user whose votes are returned
     * @return      a map from post `UUID` to vote type (`1` for upvote, `-1` for downvote)
     */
    public Map<UUID, Integer> findVotesByBoardAndUser(Board board, User user) {
        List<Post> posts =  postRepository.findAllByBoardAndStatusOrderByCreatedAtDesc(board, Status.ENABLED);
        List<PostVote> userVotes = postVoteRepository.findByUserAndPostIn(user, posts);
        return userVotes.stream()
                .collect(Collectors.toMap(
                        v -> v.getPost().getId(),
                        PostVote::getVoteType
                ));


    }

    /**
     * Retrieve the vote type for a specific post by a given user as a single-entry map.
     *
     * @param post the post to check for a vote
     * @param user the user whose vote is queried
     * @return a map containing the post's UUID mapped to the vote type (`1` for upvote, `-1` for downvote),
     *         or an empty map if the user has not voted on the post
     */
    public Map<UUID, Integer> findVoteByPostAndUser(Post post, User user) {
        Optional<PostVote> postVote = postVoteRepository.findByPostAndUser(post, user);

        if (postVote.isPresent()) {
            PostVote currentVote = postVote.get();
            return Map.of(
                    post.getId(), currentVote.getVoteType()
            );
        } else {
            return Map.of();
        }
    }

    /**
     * Disables (soft-deletes) the post identified by the given slug if the requesting user is the post's creator.
     *
     * @param slug  the slug of the post to disable
     * @param user  the user requesting the deletion; must match the post's creator
     * @throws IllegalArgumentException if the normalized slug is empty, if no post with the slug exists, or if the user is not the post's creator
     */
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
