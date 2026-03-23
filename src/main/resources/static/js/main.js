/**
 * Send a vote for a post to the server and update the post's vote UI.
 *
 * Disables vote buttons in the post while the request is in flight, posts `voteType`
 * to the board/post vote endpoint (including the page CSRF token when present), updates
 * the displayed score and active vote button based on the server response, and then
 * re-enables the buttons.
 * @param {HTMLElement} button - The clicked vote button element; must have `data-slug` and `data-board` and be inside a `.post-vote` container that contains `.post-score` and `.btn-vote` elements.
 * @param {string} voteType - The vote value sent as the `voteType` form field (e.g., `1` for upvote, `-1` for downvote).
 */
async function castVote(button, voteType) {
    const slug = button.dataset.slug;
    const board = button.dataset.board;
    const voteContainer = button.closest('.post-vote');
    const scoreEl = voteContainer.querySelector('.post-score');
    const buttons = voteContainer.querySelectorAll('.btn-vote');

    // disable buttons while request is in flight
    buttons.forEach(b => b.disabled = true);

    const params = new URLSearchParams({ voteType });

    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const response = await fetch(`/api/b/${board}/posts/${slug}/vote`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-CSRF-TOKEN': csrfToken
            },
            body: params
        });

        if (response.ok) {
            const data = await response.json();
            scoreEl.textContent = String(data.voteScore);

            const upBtn = voteContainer.querySelector('.btn-vote-up');
            const downBtn = voteContainer.querySelector('.btn-vote-down');

            upBtn.classList.remove('active');
            upBtn.setAttribute('aria-pressed', 'false');
            downBtn.classList.remove('active');
            downBtn.setAttribute('aria-pressed', 'false');


            if (data.action === 'upvoted') {
                upBtn.classList.add('active');
                upBtn.setAttribute('aria-pressed', 'true');
            }
            if (data.action === 'downvoted') {
            downBtn.classList.add('active');
            downBtn.setAttribute('aria-pressed', 'true');
            }

        } else {
        console.error("Vote failed with status:", response.status);
        alert("Could not submit vote. Please try again.");
        }
    } catch (err) {
        console.error('Vote failed', err);
    } finally {
        buttons.forEach(b => b.disabled = false);
    }
}

function toggleReply(button) {
    const replySection = button.closest('.comment-reply-section');
    const form = replySection.querySelector('.comment-reply-form');
    const isHidden = form.style.display === 'none';
    form.style.display = isHidden ? 'block' : 'none';
}

function toggleEdit(button) {
    const ownerActions = button.closest('.comment-owner-actions');
    const form = ownerActions.querySelector('.comment-edit-form');
    const editBtn = ownerActions.querySelector('button[onclick="toggleEdit(this)"]');
    const isHidden = form.style.display === 'none';
    form.style.display = isHidden ? 'block' : 'none';
    editBtn.style.display = isHidden ? 'none' : 'inline-block';
}

async function castCommentVote(button, voteType) {
    const commentId = button.dataset.commentId;
    const board = button.dataset.board;
    const slug = button.dataset.slug;
    const voteContainer = button.closest('.comment-vote');
    const scoreEl = voteContainer.querySelector('.post-score');
    const buttons = voteContainer.querySelectorAll('.btn-vote');

    buttons.forEach(b => b.disabled = true);

    const params = new URLSearchParams({ voteType });

    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const response = await fetch(`/api/b/${board}/posts/${slug}/comments/${commentId}/vote`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-CSRF-TOKEN': csrfToken
            },
            body: params
        });

        if (response.ok) {
            const data = await response.json();
            scoreEl.textContent = String(data.voteScore);

            const upBtn = voteContainer.querySelector('.btn-vote-up');
            const downBtn = voteContainer.querySelector('.btn-vote-down');

            upBtn.classList.remove('active');
            downBtn.classList.remove('active');

            if (data.action === 'upvoted') upBtn.classList.add('active');
            if (data.action === 'downvoted') downBtn.classList.add('active');
        }
    } catch (err) {
        console.error('Vote failed', err);
    } finally {
        buttons.forEach(b => b.disabled = false);
    }
}