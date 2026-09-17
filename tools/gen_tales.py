# -*- coding: utf-8 -*-
"""Emits TaleCatalog.java from the three text batches."""
import io, os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'tales'))
import tales_a, tales_b, tales_c
import extras_a, extras_b1, extras_b2, extras_c1, extras_c2
from cast import CAST

# The passages added later to lengthen the tales, spliced into the original text
# rather than tacked on the end — a tale that only grows at the end just has a
# longer goodbye.
EXTRA = {}
for _module in (extras_a, extras_b1, extras_b2, extras_c1, extras_c2):
    EXTRA.update(_module.EXTRA)


def lengthen(tid, moments):
    """Puts the added passages in, back to front so the indices stay true."""
    out = list(moments)
    additions = EXTRA.get(tid, [])
    for where, scene, text in sorted(additions, key=lambda a: -a[0]):
        assert 0 <= where <= len(moments), '%s: nowhere to put a passage at %d' % (
            tid, where)
        # A passage has to belong to the scene it is dropped between, or the picture
        # changes for one sentence and changes back.
        neighbours = {moments[i][0] for i in (where - 1, where)
                      if 0 <= i < len(moments)}
        assert scene in neighbours, (
            '%s: a passage in scene %d dropped between scenes %s'
            % (tid, scene, sorted(neighbours)))
        out.insert(where, (scene, text))
    return out

ALL = tales_a.TALES + tales_b.TALES + tales_c.TALES
OUT = ('/home/claude/pashmak_game/app/src/main/java/ir/brandimo/pashmak/'
       'data/catalog/TaleCatalog.java')

head = '''package ir.brandimo.pashmak.data.catalog;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.R;

import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.DAWN;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.DUSK;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.INDOORS;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.NIGHT;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.NOON;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.SEA;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.SNOW;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.aloft;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.desert;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.forest;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.garden;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.meadow;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.mountain;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.places;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.river;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.room;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.sea;
import static ir.brandimo.pashmak.data.catalog.TaleSceneKit.village;

/**
 * Forty tales Pashmak simply tells, each about a minute or so long.
 *
 * <p>Nothing here asks the child for anything — no taps, no right answers. The
 * interactive stories in {@link StoryCatalog} are the game; these are the wind-down,
 * for the end of the day or the back of a car. Each tale carries its own set of
 * places and moves between them as it is told.
 */
public final class TaleCatalog {

    private static final List<Tale> TALES = build();

    private TaleCatalog() {
    }

    @NonNull
    public static List<Tale> all() {
        return TALES;
    }

    @NonNull
    public static Tale tale(int index) {
        return TALES.get(Palette.wrap(index, TALES.size()));
    }

    public static int count() {
        return TALES.size();
    }

    private static Tale.Moment at(int scene, String text) {
        return new Tale.Moment(text, scene);
    }

    private static List<Tale> build() {
        List<Tale> tales = new ArrayList<>();
'''

body = []
ALL = [(tid, title, badge, scenes, lengthen(tid, moments))
       for tid, title, badge, scenes, moments in ALL]

for tid, title, badge, scenes, moments in ALL:
    places = ', '.join('%s(%s)' % (place, mood) for place, mood in scenes)
    # The subject of the tale doubles as its badge, so the card in the picker and
    # the thing moving in its scenes are the same drawing. Half the badges used to
    # be a plain circle, which told a child nothing about what they were choosing.
    hero, motion = CAST[tid]
    body.append('\n        tales.add(new Tale("%s", "%s", R.drawable.%s,'
                % (tid, title, hero))
    body.append('                R.drawable.%s, Tale.Motion.%s,' % (hero, motion))
    body.append('                places(%s),' % places)
    for i, (scene, text) in enumerate(moments):
        end = '));' if i == len(moments) - 1 else ','
        body.append('                at(%d, "%s")%s' % (scene, text, end))

tail = '''
        return Collections.unmodifiableList(tales);
    }
}
'''

io.open(OUT, 'w', encoding='utf-8').write(head + '\n'.join(body) + tail)

words = sum(len(m[1].split()) for t in ALL for m in t[4])
moments = sum(len(t[4]) for t in ALL)
print('%d tales, %d moments, %d words' % (len(ALL), moments, words))
secs = [sum(len(m[1].split()) for m in t[4]) / 110 * 60 + len(t[4]) * 0.9 for t in ALL]
print('running time: %.0fs shortest, %.0fs longest, %.0fs average'
      % (min(secs), max(secs), sum(secs) / len(secs)))
print('total listening: %.0f minutes' % (sum(secs) / 60))
ids = [t[0] for t in ALL]
assert len(set(ids)) == len(ids), 'duplicate tale id'
missing = [i for i in ids if i not in CAST]
assert not missing, 'no subject named for: %s' % missing
import glob, os
art = {os.path.basename(f)[:-4] for f in glob.glob(
    '/home/claude/pashmak_game/app/src/main/res/drawable/face_*.xml')}
absent = sorted({CAST[i][0] for i in ids} - art)
assert not absent, 'subject drawn by nothing: %s' % absent
print('ids unique: yes; every tale has a subject and every subject has a drawing')
short = [t[0] for t, sec in zip(ALL, secs) if sec < 60]
long = [t[0] for t, sec in zip(ALL, secs) if sec > 150]
assert not short, 'under a minute: %s' % short
assert not long, 'over two and a half minutes: %s' % long
print('every tale runs between one and two and a half minutes')
