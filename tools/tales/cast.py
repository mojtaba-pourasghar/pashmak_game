# -*- coding: utf-8 -*-
"""Who each told tale is about, and how that thing moves.

The tales used to have a place behind them and nothing in the place — the brave
snail's scene was an empty meadow — and half the badges were a plain circle. Each
tale now names its subject once; it is drawn into every scene of that tale and
carried onto its card in the picker, so the picture always matches the words.
"""

# id -> (drawable, motion)
CAST = {
    'moon_boat':       ('face_boat',          'DRIFT'),
    'lost_button':     ('face_button',        'SPIN'),
    'cloud_sheep':     ('face_sheep',         'DRIFT'),
    'brave_snail':     ('face_snail',         'WALK'),
    'paper_bird':      ('face_bird',          'DRIFT'),
    'shy_star':        ('face_shape_star',    'BOB'),
    'teapot_song':     ('face_teapot',        'BOB'),
    'mouse_library':   ('face_mouse',         'WALK'),
    'rain_drum':       ('face_drum',          'BOB'),
    'kite_friend':     ('face_kite',          'DRIFT'),
    'sock_pair':       ('face_sock',          'BOB'),
    'turtle_race':     ('face_turtle',        'WALK'),
    'candle_night':    ('face_candle',        'BOB'),
    'bee_garden':      ('face_bee',           'DRIFT'),
    'shadow_friend':   ('face_cat',           'WALK'),
    'fish_rainbow':    ('face_fish',          'DRIFT'),
    'bread_smell':     ('face_bread',         'BOB'),
    'wind_letter':     ('face_cloud',         'DRIFT'),
    'clock_tick':      ('face_clock',         'SPIN'),
    'puddle_sky':      ('face_drop',          'BOB'),
    'ant_crumb':       ('face_ant',           'WALK'),
    'snow_bird':       ('face_bird',          'WALK'),
    'drum_boy':        ('face_drum',          'BOB'),
    'seed_patience':   ('face_sprout',        'BOB'),
    'umbrella_share':  ('face_umbrella',      'WALK'),
    'mirror_pond':     ('face_drop',          'BOB'),
    'two_shoes':       ('face_shoe',          'WALK'),
    'night_train':     ('face_train',         'WALK'),
    'green_door':      ('face_door',          'STILL'),
    'small_wave':      ('face_drop',          'DRIFT'),
    'busy_spider':     ('face_spider',        'BOB'),
    'cold_hands':      ('face_shape_heart',   'BOB'),
    'pocket_stone':    ('face_stone',         'BOB'),
    'old_tree':        ('face_tree',          'STILL'),
    'humble_spoon':    ('face_spoon',         'BOB'),
    'first_snow_bear': ('face_bear',          'WALK'),
    'singing_well':    ('face_well',          'STILL'),
    'lantern_fish':    ('face_fish',          'DRIFT'),
    'grandma_quilt':   ('face_quilt',         'STILL'),
    'morning_rooster': ('face_rooster',       'WALK'),
}
