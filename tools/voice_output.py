#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""What every voice generator does with its output, in one place.

Three of these tools now write the same 1,136 clips into res/raw, and each one
had grown its own copy of the encoding and the clearing out. The copies drifted,
and one of them cost the app its whole voice: --fresh deleted every clip and then
discovered there was no encoder. So the two steps that are the same everywhere
live here — what can write an ogg, and what may be deleted from res/raw — and a
fix lands in all of them at once.
"""
import io
import os
import shutil
import subprocess
import sys
import wave


def encoder():
    """Whatever on this machine can write an ogg: oggenc, or ffmpeg.

    vorbis-tools is a package Windows mostly does not have and cannot install in
    one step. ffmpeg encodes the same format, is already on many machines, and
    installs with one winget line, so either one is accepted and neither is
    insisted on.
    """
    for tool in ('oggenc', 'ffmpeg'):
        if shutil.which(tool):
            return tool
    return None


NEED_ENCODER = (
    'nothing here can write an ogg. An hour and a half of speech does not ship\n'
    'as wav — it is ten times the size — and res/raw cannot hold welcome.ogg and\n'
    'welcome.wav at once: two files of one name are one resource and aapt refuses\n'
    'the build. Either of these is enough:\n'
    '\n  winget install Gyan.FFmpeg          (then open a new shell)\n'
    '  or install vorbis-tools, which brings oggenc\n'
    '\nOne line at a time — --sample, --probe — works without it.')


def encode(source, path):
    """A wav to an ogg, by whichever encoder is here. Raises if it cannot."""
    tool = encoder()
    if tool == 'oggenc':
        subprocess.check_call(['oggenc', '-Q', '-q', '4', '-o', path, source])
    elif tool == 'ffmpeg':
        subprocess.check_call(['ffmpeg', '-v', 'error', '-y', '-i', source,
                               '-c:a', 'libvorbis', '-q:a', '4', path])
    else:
        raise RuntimeError(NEED_ENCODER)
    return path


def check_encoder(scratch):
    """Prove the encoder works, here, before anything is deleted.

    Being on PATH is not the same as being able to do the job: an ffmpeg built
    without libvorbis finds the file, starts, and fails on the first clip. A
    tenth of a second of silence through the real command line answers that in
    no time at all, and this is the check that has to hold — the step after it
    deletes the app's whole voice.
    """
    if not encoder():
        sys.exit(NEED_ENCODER)
    if not os.path.isdir(scratch):
        os.makedirs(scratch)
    source, target = os.path.join(scratch, 'q.wav'), os.path.join(scratch, 'q.ogg')
    with wave.open(source, 'wb') as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(22050)
        out.writeframes(b'\0\0' * 2205)
    if os.path.exists(target):
        os.unlink(target)
    try:
        encode(source, target)
    except Exception as problem:                           # noqa: BLE001
        sys.exit('%s is on PATH but could not write an ogg:\n  %s\n\n%s'
                 % (encoder(), ' '.join(str(problem).split())[:200], NEED_ENCODER))
    if not os.path.exists(target) or not os.path.getsize(target):
        sys.exit('%s produced no ogg from a test clip.\n\n%s'
                 % (encoder(), NEED_ENCODER))


def rebuild_lines(root):
    """The line list, made again from the hand-vowelised manifest."""
    print('rebuilding the line list from audio_manifest.txt')
    build = os.path.join(root, 'tools/build_voice_lines.py')
    done = subprocess.run([sys.executable, build], capture_output=True, text=True)
    if done.returncode:
        sys.exit('could not rebuild the line list:\n%s'
                 % (done.stderr or done.stdout).strip())
    for row in (done.stdout or '').strip().splitlines():
        print('  %s' % row)


def clear_spoken(raw, names):
    """Delete the spoken clips from res/raw, and nothing else in it.

    "Delete everything in res/raw" cannot be taken literally, and the reason is
    the request it usually comes with: audio_manifest.txt lives in that folder
    and is the hand-vowelised source every clip is generated from. So do the ten
    bought lullaby recordings, which no model can make again, and the thirteen
    sound effects and music beds.

    So the files to delete are named from the line list — <name>.ogg or .wav for
    each line, whatever the last voice left behind — and everything else is safe
    by construction rather than by a list that can fall out of date. .mp3 is left
    out entirely, so no name clash can reach a bought recording. What stays is
    printed, because this is the one step that running a tool again cannot undo.
    """
    gone = 0
    kept = []
    for entry in sorted(os.listdir(raw)):
        stem, ext = os.path.splitext(entry)
        if stem in names and ext.lower() in ('.ogg', '.wav'):
            os.unlink(os.path.join(raw, entry))
            gone += 1
        else:
            kept.append(entry)
    print('\ndeleted %d spoken clips, kept %d other files in res/raw:'
          % (gone, len(kept)))
    for entry in kept:
        print('   %s' % entry)
    return gone, kept
