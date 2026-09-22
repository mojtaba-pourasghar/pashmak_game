# -*- coding: utf-8 -*-
"""Which of Pashmak's moods each line is said in.

One table, shared by every voice generator, because the routing is the part that
is wrong in a way you can only hear. Two faults came out of getting it wrong
once already:

  * story_<id>_<n>_yes is the praise a story gives for finding the right thing.
    It was being read in the calm storytelling voice — the one moment in a story
    that must not sound calm.
  * 'night_' looked for anywhere in a name also matches tale_candle_night_*, so
    thirty passages of the candle tale were read as a bedtime whisper.

So a rule says how it matches: `starts` matches the beginning of the name and
nothing else, and `contains` holds only the handful of things that genuinely live
in the middle of one.
"""

# (mood, prefixes, substrings) — the first rule that matches wins.
RULES = [
    # A child has just got something right and should hear it.
    ('delighted',
     ('win', 'memory_match', 'memory_win', 'paint_right', 'paint_done',
      'trace_done', 'bubble_pop', 'bubble_round', 'mission_done', 'item_arrived',
      'story_end', 'tale_done', 'giggle'),
     ('_yes',)),

    # They got it wrong, and nothing here is allowed to sound disappointed.
    ('kind',
     ('try_again', 'memory_miss', 'paint_wrong', 'trace_more', 'story_wrong',
      'bubble_wrong', 'draw_empty', 'stage_locked'),
     ()),

    # The slowest and softest thing in the app.
    ('bedtime', ('night_', 'lullaby'), ()),

    # A letter or a digit on its own: said slowly, and only once.
    ('glyph', ('letter_', 'digit_'), ()),

    # Telling a story: measured, word by word, room to picture it.
    ('story', ('story_', 'tale_'), ()),
]

# Everything else: the mascot talking during a game. Warm and unhurried.
DEFAULT = 'game'


def mood_for(name):
    """The mood a clip is spoken in, by clip name."""
    for mood, starts, contains in RULES:
        if name.startswith(starts) or any(bit in name for bit in contains):
            return mood
    return DEFAULT


def tally(names):
    """How many lines land in each mood — the cheap way to catch a bad rule."""
    counts = {}
    for name in names:
        mood = mood_for(name)
        counts[mood] = counts.get(mood, 0) + 1
    return counts
