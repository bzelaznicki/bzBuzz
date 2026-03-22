/**
 * Send a vote for a post to the server and update the post's vote UI.
 *
 * Disables vote buttons in the post while the request is in flight, posts `voteType`
 * to the board/post vote endpoint (including the page CSRF token when present), updates
 * the displayed score and active vote button based on the server response, and then
 * re-enables the buttons.
 * @param {HTMLElement} button - The clicked vote button element; must have `data-slug` and `data-board` and be inside a `.post-vote` container that contains `.post-score` and `.btn-vote` elements.
 * @param {string} voteType - The vote action value sent as the `voteType` form field (e.g., `"upvoted"` or `"downvoted"`).
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