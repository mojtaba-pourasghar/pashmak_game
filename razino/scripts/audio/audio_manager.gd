extends Node
## Dialogue, music and effects on three independent buses (GDD §12).
##
## A missing audio file is an expected condition, not a crash: the voice-over
## for a line may not be recorded yet, or a downloaded chapter may be missing
## a clip. In that case `voice_finished` still fires after an estimated
## reading time, so subtitles and scene flow never stall. That estimate is
## clearly a fallback, not a substitute for a recorded line.

signal voice_started(audio_id: String)
signal voice_finished(audio_id: String)
signal music_changed(track_id: String)

const BUS_VOICE := "Voice"
const BUS_MUSIC := "Music"
const BUS_SFX := "SFX"

## Reading pace used only when a clip is absent, in characters per second.
const FALLBACK_CPS := 13.0
const FALLBACK_MIN_SECONDS := 1.4

var _voice_player: AudioStreamPlayer
var _music_player: AudioStreamPlayer
var _sfx_players: Array[AudioStreamPlayer] = []
var _current_voice_id: String = ""
var _fallback_timer: SceneTreeTimer = null
var _current_music: String = ""

## Audio ids that had no file. Surfaced by the content validator and the
## in-game diagnostics screen so gaps are visible rather than silent.
var missing_audio: Dictionary = {}

func _ready() -> void:
	_ensure_buses()
	_voice_player = AudioStreamPlayer.new()
	_voice_player.bus = BUS_VOICE
	_voice_player.finished.connect(_on_voice_player_finished)
	add_child(_voice_player)

	_music_player = AudioStreamPlayer.new()
	_music_player.bus = BUS_MUSIC
	add_child(_music_player)

	for i in 6:
		var p := AudioStreamPlayer.new()
		p.bus = BUS_SFX
		add_child(p)
		_sfx_players.append(p)

	Settings.changed.connect(_on_settings_changed)
	apply_volumes()

func _ensure_buses() -> void:
	for bus_name in [BUS_VOICE, BUS_MUSIC, BUS_SFX]:
		if AudioServer.get_bus_index(bus_name) == -1:
			var idx := AudioServer.bus_count
			AudioServer.add_bus(idx)
			AudioServer.set_bus_name(idx, bus_name)
			AudioServer.set_bus_send(idx, "Master")

func apply_volumes() -> void:
	_set_bus_volume(BUS_VOICE, Settings.voice_volume)
	_set_bus_volume(BUS_MUSIC, Settings.music_volume)
	_set_bus_volume(BUS_SFX, Settings.sfx_volume)

func _set_bus_volume(bus_name: String, linear: float) -> void:
	var idx := AudioServer.get_bus_index(bus_name)
	if idx == -1:
		return
	AudioServer.set_bus_mute(idx, linear <= 0.001)
	AudioServer.set_bus_volume_db(idx, linear_to_db(maxf(linear, 0.0001)))

func _on_settings_changed(key: String) -> void:
	if key.ends_with("_volume"):
		apply_volumes()

# — voice ————————————————————————————————————————————————

## `subtitle` is used only to size the fallback timer when no clip exists.
func play_voice(audio_id: String, subtitle: String = "") -> void:
	stop_voice()
	if audio_id.is_empty():
		_start_fallback(audio_id, subtitle)
		return
	var stream := _load_audio(_voice_path(audio_id))
	if stream == null:
		missing_audio[audio_id] = true
		Log.warn("Audio", "no clip for %s — using estimated timing" % audio_id)
		_start_fallback(audio_id, subtitle)
		return
	_current_voice_id = audio_id
	_voice_player.stream = stream
	_voice_player.play()
	voice_started.emit(audio_id)

func stop_voice() -> void:
	if _fallback_timer != null:
		if _fallback_timer.timeout.is_connected(_on_fallback_timeout):
			_fallback_timer.timeout.disconnect(_on_fallback_timeout)
		_fallback_timer = null
	if _voice_player != null and _voice_player.playing:
		_voice_player.stop()
	_current_voice_id = ""

func is_voice_playing() -> bool:
	return (_voice_player != null and _voice_player.playing) or _fallback_timer != null

## How long the current line should stay on screen. Real clip length when
## there is one, otherwise the reading estimate.
func estimate_seconds(subtitle: String) -> float:
	return maxf(FALLBACK_MIN_SECONDS, subtitle.length() / FALLBACK_CPS)

func _start_fallback(audio_id: String, subtitle: String) -> void:
	_current_voice_id = audio_id
	voice_started.emit(audio_id)
	var seconds := estimate_seconds(subtitle)
	_fallback_timer = get_tree().create_timer(seconds)
	_fallback_timer.timeout.connect(_on_fallback_timeout)

func _on_fallback_timeout() -> void:
	_fallback_timer = null
	var id := _current_voice_id
	_current_voice_id = ""
	voice_finished.emit(id)

func _on_voice_player_finished() -> void:
	var id := _current_voice_id
	_current_voice_id = ""
	voice_finished.emit(id)

# — music & sfx ——————————————————————————————————————————

func play_music(track_id: String, fade_seconds: float = 1.0) -> void:
	if track_id == _current_music:
		return
	var stream := _load_audio(_music_path(track_id))
	if stream == null:
		missing_audio[track_id] = true
		Log.warn("Audio", "no music track %s" % track_id)
		return
	if stream is AudioStreamOggVorbis:
		(stream as AudioStreamOggVorbis).loop = true
	_current_music = track_id
	if _music_player.playing and fade_seconds > 0.0 and not Settings.reduce_motion:
		var t := create_tween()
		t.tween_property(_music_player, "volume_db", -40.0, fade_seconds * 0.5)
		await t.finished
	_music_player.stream = stream
	_music_player.volume_db = 0.0
	_music_player.play()
	music_changed.emit(track_id)

func stop_music() -> void:
	_music_player.stop()
	_current_music = ""

func play_sfx(sfx_id: String) -> void:
	var stream := _load_audio(_sfx_path(sfx_id))
	if stream == null:
		missing_audio[sfx_id] = true
		return
	for p in _sfx_players:
		if not p.playing:
			p.stream = stream
			p.play()
			return
	# All voices busy: reuse the first rather than dropping the cue.
	_sfx_players[0].stream = stream
	_sfx_players[0].play()

# — resolution ———————————————————————————————————————————

func _voice_path(audio_id: String) -> String:
	return "%s/vo/%s.ogg" % [_audio_root(), audio_id]

func _music_path(track_id: String) -> String:
	return "%s/music/%s.ogg" % [_audio_root(), track_id]

func _sfx_path(sfx_id: String) -> String:
	return "%s/sfx/%s.ogg" % [_audio_root(), sfx_id]

func _audio_root() -> String:
	return "res://assets/audio"

func _load_audio(path: String) -> AudioStream:
	# Bundled assets go through the resource loader; downloaded chapter audio
	# lives outside res:// and is read from disk.
	if ResourceLoader.exists(path):
		var res := ResourceLoader.load(path)
		if res is AudioStream:
			return res
	var external := ContentManager.resolve_external_audio(path)
	if external != "" and FileAccess.file_exists(external):
		return AudioStreamOggVorbis.load_from_file(external)
	return null

func missing_audio_ids() -> PackedStringArray:
	var out := PackedStringArray()
	for k in missing_audio.keys():
		out.append(String(k))
	return out
