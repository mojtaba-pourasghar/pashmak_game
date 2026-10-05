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


# How long each mood takes, as a length scale: 1.0 is the model's own pace and
# larger is slower. It is written that way because that is what a VITS model
# takes; sherpa-onnx wants the reciprocal, so ask it for speed_for() instead of
# inverting this by hand in two places.
PACE = {
    'delighted': 0.95,
    'kind': 1.18,
    'bedtime': 1.40,
    'glyph': 1.45,
    'story': 1.18,
    'game': 1.08,
}


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


def pace_for(name):
    """The length scale for a clip: larger is slower."""
    return PACE[mood_for(name)]


def speed_for(name):
    """The same pace the other way up, which is what sherpa-onnx asks for."""
    return 1.0 / PACE[mood_for(name)]


# Who is speaking, on every single line. This is the "middle pitch and warm"
# part, said once so it cannot drift between moods.
PERSONA = (
    'You are Pashmak, a small soft kind boy bear — a chubby, sweet cartoon mascot '
    'who talks to a Persian-speaking child aged three to eight. Speak Persian in a '
    'warm, mid-pitched voice: not high and not deep. Be natural and expressive, '
    'clear enough for a small child to catch every single word, and never shouty, '
    'never sing-song, never baby talk.'
)

# And how this particular line is said. Directed in words, which is the whole
# point of using this engine.
MOOD_DIRECTION = {
    'delighted': 'Sound genuinely delighted and proud of the child, bright and '
                 'smiling, lifting at the end.',
    'kind': 'Sound kind and reassuring. The child has just got something wrong, '
            'so there must be no trace of disappointment — only warmth and a '
            'nudge to try again.',
    'bedtime': 'Almost a whisper. Very slow and very soft, as if the child is '
               'already half asleep.',
    'glyph': 'Say this one letter slowly and very clearly, just once, with a '
             'small friendly lilt.',
    'story': 'Tell this the way a storyteller would: unhurried and measured, '
             'with room after each phrase for the child to picture it.',
    'game': 'Warm, unhurried and companionable, like a friend sitting beside them.',
}


def direction(name):
    return '%s %s' % (PERSONA, MOOD_DIRECTION[mood_for(name)])
