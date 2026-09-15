# -*- coding: utf-8 -*-
"""A plain check of RoiMapper's arithmetic, for both PreviewView scale types.

RoiMapper's own header calls it "the piece most likely to be subtly wrong", and it
had no test. It assumed FIT_CENTER while the layout left PreviewView on its
FILL_CENTER default, so the scan frame mapped onto the wrong part of the photo and
every capture failed. This mirrors the Java arithmetic and asserts the property that
actually matters: a frame drawn around a thing on screen must land on that same
thing in the image.
"""
import sys

def to_normalized(roi, view_w, view_h, img_w, img_h, mode):
    """Mirrors RoiMapper.toNormalized for the given mode ('fit' or 'fill')."""
    left, top, right, bottom = roi
    pick = min if mode == 'fit' else max
    scale = pick(view_w / img_w, view_h / img_h)
    shown_w, shown_h = img_w * scale, img_h * scale
    off_x, off_y = (view_w - shown_w) / 2, (view_h - shown_h) / 2
    clamp = lambda v: 0.0 if v < 0 else (1.0 if v > 1 else v)
    out = (clamp((left - off_x) / shown_w), clamp((top - off_y) / shown_h),
           clamp((right - off_x) / shown_w), clamp((bottom - off_y) / shown_h))
    if out[2] - out[0] <= 0.02 or out[3] - out[1] <= 0.02:
        return (0.0, 0.0, 1.0, 1.0)
    return out

def forward(nx, ny, view_w, view_h, img_w, img_h, mode):
    """Where image point (nx, ny) lands on screen — the inverse of the mapping."""
    pick = min if mode == 'fit' else max
    scale = pick(view_w / img_w, view_h / img_h)
    shown_w, shown_h = img_w * scale, img_h * scale
    off_x, off_y = (view_w - shown_w) / 2, (view_h - shown_h) / 2
    return off_x + nx * shown_w, off_y + ny * shown_h

def run():
    # A landscape phone showing a 4:3 capture: the two modes differ a lot here.
    view_w, view_h = 640, 360
    img_w, img_h = 1600, 1200
    roi = (160, 60, 480, 300)            # the guide frame, 8% inset-ish
    failures = []

    for mode in ('fit', 'fill'):
        nx0, ny0, nx1, ny1 = to_normalized(roi, view_w, view_h, img_w, img_h, mode)
        # Round-trip: the normalised rect must map back onto the frame we drew.
        bx0, by0 = forward(nx0, ny0, view_w, view_h, img_w, img_h, mode)
        bx1, by1 = forward(nx1, ny1, view_w, view_h, img_w, img_h, mode)
        drift = max(abs(bx0 - roi[0]), abs(by0 - roi[1]),
                    abs(bx1 - roi[2]), abs(by1 - roi[3]))
        ok = drift < 0.5
        print('  %-5s -> (%.3f, %.3f, %.3f, %.3f)   round-trip drift %.2fpx  %s'
              % (mode, nx0, ny0, nx1, ny1, drift, 'ok' if ok else 'WRONG'))
        if not ok:
            failures.append(mode)

    # The point of the bug: reading a FILL_CENTER preview with the FIT_CENTER
    # formula does not just shift slightly, it lands somewhere else entirely.
    fit = to_normalized(roi, view_w, view_h, img_w, img_h, 'fit')
    fill = to_normalized(roi, view_w, view_h, img_w, img_h, 'fill')
    gap = max(abs(a - b) for a, b in zip(fit, fill))
    print('\n  largest disagreement between the two formulas: %.3f of the image' % gap)
    if gap < 0.05:
        failures.append('modes indistinguishable - the check would not catch a mix-up')

    print('\n%s' % ('FAIL: ' + ', '.join(failures) if failures
                    else 'PASS: both scale types map the frame onto itself'))
    return 1 if failures else 0

if __name__ == '__main__':
    sys.exit(run())
