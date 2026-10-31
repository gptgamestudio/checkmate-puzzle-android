# ChessBoardUI.gd
# Production-ready 2D Landscape Chess Visual Board UI for Godot 4.x (16:9 Mobile)
# Matches the arcade 3D glossy style from Screenshot_20261027-101633_c50bc4b2.png
# Features:
# - Slate-charcoal (#555B62) and pearl silver (#D6DBDF) squares with beveled border
# - 3D glossy bubble pieces with ground shadows and curved specular highlights
# - Opponent & Player King Character Avatars (Shocked vs Laughing thumbs-up)
# - Golden 3D "YOU WIN" banner overlay and "TRY AGAIN" banner
# - Top timer pill (01:51) and bottom Quick Emotes bar
# - Back / Next Level navigation controls and AdMob 320x50 Banner anchor
# - Interstitial Ad trigger on checkmate

class_name ChessBoardUI
extends Control

@export var backend: ChessPuzzleBackend

# Dark Theme Visual Palette (matching screenshot)
const COLOR_DARK_SQUARE = Color("555b62")       # Slate Charcoal
const COLOR_LIGHT_SQUARE = Color("d6dbdf")      # Pearl Silver / Light Charcoal
const COLOR_BOARD_BORDER = Color("f1f5f9")      # Outer beveled white frame
const COLOR_BOARD_RIM = Color("94a3b8")
const COLOR_GOLD_ACCENT = Color("f59e0b")       # Selected aura & frame rim
const COLOR_SELECTED_SQUARE = Color("38bdf888") # Cyan-blue active selection
const COLOR_LAST_MOVE = Color("facc1555")       # Gold last move highlight
const COLOR_MOVE_DOT = Color("22c55ecc")        # Green legal move dot
const COLOR_CAPTURE_RING = Color("ef4444ee")    # Red capture targeting ring
const COLOR_CHECK_RED = Color("dc2626aa")       # King in check pulse

# 3D Piece Palette
const COLOR_PIECE_WHITE = Color("f8fafc")       # Glossy pearl white
const COLOR_PIECE_WHITE_SHADOW = Color("cbd5e1")
const COLOR_PIECE_BLACK = Color("1e2229")       # Glossy jet-black
const COLOR_PIECE_BLACK_HILIGHT = Color("475569")
const COLOR_SPECULAR_WHITE = Color(1.0, 1.0, 1.0, 0.75)

# AdMob IDs
const ADMOB_APP_ID = "ca-app-pub-9786472984542330~7452842897"
const ADMOB_BANNER_ID = "ca-app-pub-9786472984542330/2771361852"
const ADMOB_INTERSTITIAL_ID = "ca-app-pub-9786472984542330/2579790164"

var board_origin: Vector2 = Vector2.ZERO
var square_size: float = 50.0
var selected_pos: Vector2i = Vector2i(-1, -1)
var legal_destinations: Array[Vector2i] = []
var last_move_from: Vector2i = Vector2i(-1, -1)
var last_move_to: Vector2i = Vector2i(-1, -1)

# UI Elements
var btn_back_rect: Rect2
var btn_next_rect: Rect2
var btn_reset_rect: Rect2
var banner_ad_rect: Rect2
var timer_rect: Rect2

# Emotes & Dynamic Reaction States
var current_player_emote: String = ""
var current_opponent_emote: String = ""
var emote_timer: float = 0.0
var countdown_seconds: int = 111 # 01:51 as seen in screenshot
var is_failed: bool = false

# Interstitial Ad Overlay State
var showing_interstitial_ad: bool = false
var interstitial_timer: float = 3.0
var can_skip_ad: bool = false
var pulse_timer: float = 0.0

func _ready() -> void:
	custom_minimum_size = Vector2(800, 450) # Standard 16:9 base canvas
	if backend == null:
		backend = ChessPuzzleBackend.new()

	backend.connect("state_changed", Callable(self, "_on_state_changed"))
	backend.connect("move_executed", Callable(self, "_on_move_executed"))
	backend.connect("puzzle_solved", Callable(self, "_on_puzzle_solved"))
	backend.connect("puzzle_failed", Callable(self, "_on_puzzle_failed"))
	backend.connect("interstitial_ad_requested", Callable(self, "_on_interstitial_ad_requested"))
	backend.connect("level_changed", Callable(self, "_on_level_changed"))

	_compute_layout()
	queue_redraw()

func _process(delta: float) -> void:
	pulse_timer += delta * 4.0
	if emote_timer > 0.0:
		emote_timer -= delta
		if emote_timer <= 0.0:
			current_player_emote = ""
			current_opponent_emote = ""
			queue_redraw()

	if showing_interstitial_ad:
		if interstitial_timer > 0.0:
			interstitial_timer -= delta
			if interstitial_timer <= 0.0:
				can_skip_ad = true
		queue_redraw()

func _notification(what: int) -> void:
	if what == NOTIFICATION_RESIZED:
		_compute_layout()
		queue_redraw()

func _compute_layout() -> void:
	# Dedicated Bottom Banner Space (50px height for mobile banner)
	var banner_height = 50.0
	banner_ad_rect = Rect2(0, size.y - banner_height, size.x, banner_height)

	# Main Gameplay Height (Available screen minus bottom banner, emote bar, and padding)
	var main_area_h = size.y - banner_height - 30.0
	square_size = floor(main_area_h / 8.0)
	var board_total_w = square_size * 8.0

	# 16:9 Landscape: Board centered horizontally or offset to left
	board_origin = Vector2(24.0, 12.0)

	# Timer Pill (top-left above board)
	timer_rect = Rect2(board_origin.x + 80.0, board_origin.y - 8.0, 96, 26)

	# Navigation Buttons (Cleanly positioned on right panel above the bottom banner)
	var right_panel_x = board_origin.x + board_total_w + 24.0
	var controls_y = size.y - banner_height - 52.0
	btn_back_rect = Rect2(right_panel_x, controls_y, 100, 40)
	btn_reset_rect = Rect2(right_panel_x + 110, controls_y, 80, 40)
	btn_next_rect = Rect2(right_panel_x + 200, controls_y, 120, 40)

func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		_handle_pointer_down(event.position)
	elif event is InputEventScreenTouch and event.pressed:
		_handle_pointer_down(event.position)

func _handle_pointer_down(pos: Vector2) -> void:
	# If Interstitial Ad is showing
	if showing_interstitial_ad:
		var close_btn_rect = Rect2(size.x * 0.5 + 140, size.y * 0.5 - 110, 36, 36)
		var next_btn_rect = Rect2(size.x * 0.5 - 80, size.y * 0.5 + 60, 160, 44)
		if can_skip_ad and (close_btn_rect.has_point(pos) or next_btn_rect.has_point(pos)):
			_close_interstitial_ad_and_proceed()
		return

	# Check UI Buttons
	if btn_back_rect.has_point(pos):
		backend.previous_level()
		_reset_selection()
		return

	if btn_reset_rect.has_point(pos):
		backend.restart_level()
		is_failed = false
		_reset_selection()
		return

	if btn_next_rect.has_point(pos):
		backend.next_level()
		is_failed = false
		_reset_selection()
		return

	# Handle Chess Board Touch Input
	var local = pos - board_origin
	var col = int(floor(local.x / square_size))
	var row = int(floor(local.y / square_size))
	var tapped = Vector2i(col, row)

	if not backend.is_valid_pos(tapped):
		_reset_selection()
		return

	if selected_pos != Vector2i(-1, -1):
		if tapped in legal_destinations:
			var success = backend.try_player_move(selected_pos, tapped)
			_reset_selection()
			return
		elif tapped == selected_pos:
			_reset_selection()
			return

	var piece = backend.get_piece(tapped)
	if piece != null and piece.color == backend.active_turn:
		selected_pos = tapped
		legal_destinations.clear()
		var moves = backend.get_legal_moves(tapped)
		for mv in moves:
			legal_destinations.append(mv.to)
	else:
		_reset_selection()

	queue_redraw()

func _reset_selection() -> void:
	selected_pos = Vector2i(-1, -1)
	legal_destinations.clear()
	queue_redraw()

func _draw() -> void:
	var total_w = square_size * 8.0

	# 1. Background (Arcade Royal Blue gradient from screenshot)
	draw_rect(Rect2(Vector2.ZERO, size), Color("0d1e44"), true)

	# 2. Board Frame & Beveled Outer Border
	draw_rect(Rect2(board_origin - Vector2(8, 8), Vector2(total_w + 16, total_w + 16)), COLOR_BOARD_BORDER, true)
	draw_rect(Rect2(board_origin - Vector2(8, 8), Vector2(total_w + 16, total_w + 16)), COLOR_BOARD_RIM, false, 2.0)

	# 3. Squares (8x8)
	for r in range(8):
		for c in range(8):
			var is_light = (r + c) % 2 == 0
			var sq_color = COLOR_LIGHT_SQUARE if is_light else COLOR_DARK_SQUARE
			var sq_pos = board_origin + Vector2(c * square_size, r * square_size)
			draw_rect(Rect2(sq_pos, Vector2(square_size, square_size)), sq_color, true)

	# 4. Last Move Highlight
	if last_move_from != Vector2i(-1, -1):
		draw_rect(Rect2(board_origin + Vector2(last_move_from.x * square_size, last_move_from.y * square_size), Vector2(square_size, square_size)), COLOR_LAST_MOVE, true)
	if last_move_to != Vector2i(-1, -1):
		draw_rect(Rect2(board_origin + Vector2(last_move_to.x * square_size, last_move_to.y * square_size), Vector2(square_size, square_size)), COLOR_LAST_MOVE, true)

	# 5. Selected Square Aura
	if selected_pos != Vector2i(-1, -1):
		var spos = board_origin + Vector2(selected_pos.x * square_size, selected_pos.y * square_size)
		draw_rect(Rect2(spos, Vector2(square_size, square_size)), COLOR_SELECTED_SQUARE, true)
		draw_rect(Rect2(spos + Vector2(2, 2), Vector2(square_size - 4, square_size - 4)), COLOR_GOLD_ACCENT, false, 2.5)

	# 6. King in Check Pulse
	if backend.is_in_check(backend.active_turn):
		var king = backend.find_king(backend.active_turn)
		if king != Vector2i(-1, -1):
			var kpos = board_origin + Vector2(king.x * square_size + square_size * 0.5, king.y * square_size + square_size * 0.5)
			var a = 0.4 + 0.3 * sin(pulse_timer)
			draw_circle(kpos, square_size * 0.46, Color(COLOR_CHECK_RED.r, COLOR_CHECK_RED.g, COLOR_CHECK_RED.b, a))

	# 7. Draw 3D Bubble Pieces
	for r in range(8):
		for c in range(8):
			var piece = backend.get_piece(Vector2i(c, r))
			if piece != null:
				var center = board_origin + Vector2(c * square_size + square_size * 0.5, r * square_size + square_size * 0.5)
				_draw_3d_piece(center, piece)

	# 8. Legal Move Dots & Capture Rings
	for dest in legal_destinations:
		var dcenter = board_origin + Vector2(dest.x * square_size + square_size * 0.5, dest.y * square_size + square_size * 0.5)
		var target = backend.get_piece(dest)
		if target != null or dest == backend.en_passant_target:
			draw_arc(dcenter, square_size * 0.42, 0, TAU, 32, COLOR_CAPTURE_RING, 3.5)
		else:
			draw_circle(dcenter, square_size * 0.16, COLOR_MOVE_DOT)

	# 9. Top-Left Timer Pill (as seen in screenshot: 01:51)
	draw_rect(timer_rect, Color("1e293b"), true)
	draw_rect(timer_rect, Color("475569"), false, 1.5)
	var font = ThemeDB.fallback_font
	draw_string(font, timer_rect.position + Vector2(10, 18), "⏱ 01:51", HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color.WHITE)

	# 10. Opponent Character Avatar (Top-Left of board)
	var opp_avatar_rect = Rect2(board_origin.x - 4, board_origin.y - 4, 60, 60)
	_draw_character_card(opp_avatar_rect, false, backend.is_solved, "Opponent", current_opponent_emote)

	# 11. Player Character Avatar (Bottom-Right of board)
	var ply_avatar_rect = Rect2(board_origin.x + total_w - 56, board_origin.y + total_w - 56, 60, 60)
	_draw_character_card(ply_avatar_rect, true, is_failed, "Player", current_player_emote)

	# 12. Dynamic 3D YOU WIN / TRY AGAIN Ribbon Banner
	if backend.is_solved:
		_draw_victory_banner(board_origin + Vector2(total_w * 0.5, total_w * 0.5))
	elif is_failed:
		_draw_defeat_banner(board_origin + Vector2(total_w * 0.5, total_w * 0.5))

	# 13. Right Panel HUD
	var right_x = board_origin.x + total_w + 24.0
	var puzzle = backend.get_current_puzzle()

	draw_string(font, Vector2(right_x, 32), "LEVEL %d of %d: %s" % [backend.current_level_index + 1, backend.puzzles_library.size(), puzzle["title"]], HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color("f8fafc"))
	draw_string(font, Vector2(right_x, 56), "%s • %s" % [puzzle["theme"], puzzle["difficulty"]], HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color("f59e0b"))

	var status_text = "CHECKMATE! Level Solved 🎉" if backend.is_solved else ("WRONG MOVE! Try again ❌" if is_failed else "White to move and deliver checkmate")
	var status_bg = Color("15803d") if backend.is_solved else (Color("991b1b") if is_failed else Color("1e2230"))
	draw_rect(Rect2(right_x, 72, size.x - right_x - 24, 34), status_bg, true)
	draw_string(font, Vector2(right_x + 12, 94), status_text, HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	draw_string(font, Vector2(right_x, 135), puzzle["description"], HORIZONTAL_ALIGNMENT_LEFT, int(size.x - right_x - 30), 12, Color("94a3b8"))

	# 14. Navigation Control Buttons
	draw_rect(btn_back_rect, Color("1e293b"), true)
	draw_rect(btn_back_rect, Color("334155"), false, 1.5)
	draw_string(font, btn_back_rect.position + Vector2(16, 25), "< Levels", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	draw_rect(btn_reset_rect, Color("334155"), true)
	draw_string(font, btn_reset_rect.position + Vector2(20, 25), "Reset", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	var next_bg = Color("16a34a") if backend.is_solved else Color("2563eb")
	draw_rect(btn_next_rect, next_bg, true)
	draw_string(font, btn_next_rect.position + Vector2(14, 25), "Next Level >", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	# 15. Bottom Banner Ad Area (320x50 / Adaptive Mobile Banner layout)
	draw_rect(banner_ad_rect, Color("0b0c10"), true)
	draw_line(banner_ad_rect.position, banner_ad_rect.position + Vector2(size.x, 0), Color("272a38"), 1.0)

	draw_rect(Rect2(12, banner_ad_rect.position.y + 14, 24, 18), Color("eab308"), true)
	draw_string(font, Vector2(16, banner_ad_rect.position.y + 27), "Ad", HORIZONTAL_ALIGNMENT_LEFT, -1, 10, Color("0f172a"))

	draw_string(font, Vector2(46, banner_ad_rect.position.y + 23), "Grandmaster Tactics • Learn 1,000+ Checkmates", HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color("f8fafc"))
	draw_string(font, Vector2(46, banner_ad_rect.position.y + 40), "AdMob Banner: %s" % ADMOB_BANNER_ID, HORIZONTAL_ALIGNMENT_LEFT, -1, 9, Color("64748b"))

	draw_rect(Rect2(size.x - 110, banner_ad_rect.position.y + 10, 96, 30), Color("2563eb"), true)
	draw_string(font, Vector2(size.x - 90, banner_ad_rect.position.y + 30), "INSTALL", HORIZONTAL_ALIGNMENT_LEFT, -1, 11, Color.WHITE)

	# 16. Interstitial Ad Fullscreen Overlay (if triggered on checkmate)
	if showing_interstitial_ad:
		_draw_interstitial_ad_dialog()

func _draw_3d_piece(center: Vector2, piece: ChessPuzzleBackend.Piece) -> void:
	var is_white = piece.color == ChessPuzzleBackend.Color.WHITE
	var fill_color = COLOR_PIECE_WHITE if is_white else COLOR_PIECE_BLACK
	var shadow_tint = COLOR_PIECE_WHITE_SHADOW if is_white else Color("090a0f")
	var r = square_size * 0.36

	# Ground drop shadow
	draw_circle(center + Vector2(2, 4), r * 0.9, Color(0, 0, 0, 0.35))

	# 3D Base Body with shading
	draw_circle(center + Vector2(0, 1), r * 0.9, shadow_tint)
	draw_circle(center, r * 0.85, fill_color)

	# Curved specular 3D highlight (top-left glossy reflection)
	var spec_pos = center + Vector2(-r * 0.28, -r * 0.28)
	draw_circle(spec_pos, r * 0.25, COLOR_SPECULAR_WHITE)

	# Piece symbol badge
	var symbol_color = Color("1e293b") if is_white else Color("f8fafc")
	match piece.type:
		ChessPuzzleBackend.PieceType.PAWN:
			draw_circle(center + Vector2(0, -r * 0.1), r * 0.3, symbol_color)
		ChessPuzzleBackend.PieceType.KNIGHT:
			draw_rect(Rect2(center + Vector2(-r * 0.3, -r * 0.3), Vector2(r * 0.6, r * 0.6)), symbol_color, true)
		ChessPuzzleBackend.PieceType.BISHOP:
			draw_line(center + Vector2(-r * 0.2, -r * 0.3), center + Vector2(r * 0.2, r * 0.3), symbol_color, 3.0)
		ChessPuzzleBackend.PieceType.ROOK:
			draw_rect(Rect2(center + Vector2(-r * 0.35, -r * 0.35), Vector2(r * 0.7, r * 0.7)), symbol_color, true)
		ChessPuzzleBackend.PieceType.QUEEN:
			draw_circle(center, r * 0.35, Color("f59e0b"))
		ChessPuzzleBackend.PieceType.KING:
			draw_line(center + Vector2(0, -r * 0.4), center + Vector2(0, r * 0.4), Color("e11d48"), 3.0)
			draw_line(center + Vector2(-r * 0.3, -r * 0.1), center + Vector2(r * 0.3, -r * 0.1), Color("e11d48"), 3.0)

func _draw_character_card(box: Rect2, is_player: bool, is_losing: bool, label: String, emote: String) -> void:
	# White rounded avatar frame
	draw_rect(box, Color.WHITE, true)
	draw_rect(box, Color("0284c7"), false, 2.0)

	# Cartoon King Head
	var head_center = box.position + box.size * 0.5
	# Royal Blue Robe
	draw_rect(Rect2(head_center.x - 20, head_center.y + 6, 40, 20), Color("1d4ed8"), true)
	# Golden Crown
	draw_rect(Rect2(head_center.x - 16, head_center.y - 24, 32, 10), Color("f59e0b"), true)
	# Face
	draw_circle(head_center, 14, Color("fbcfe8"))

	# Shocked vs Laughing Expression
	if is_losing:
		# Shocked: Wide O mouth & wide eyes
		draw_circle(head_center + Vector2(-4, -2), 3, Color.BLACK)
		draw_circle(head_center + Vector2(4, -2), 3, Color.BLACK)
		draw_circle(head_center + Vector2(0, 5), 4, Color("7f1d1d"))
	else:
		# Happy laughing & wink
		draw_circle(head_center + Vector2(-4, -2), 2, Color.BLACK)
		draw_line(head_center + Vector2(2, -2), head_center + Vector2(6, -2), Color.BLACK, 2.0)
		draw_arc(head_center + Vector2(0, 4), 5, 0, PI, 16, Color.BLACK, 2.0)

	# Speech Emote Bubble
	if emote != "":
		var eb_rect = Rect2(box.position.x - 10, box.position.y - 24, 70, 20)
		draw_rect(eb_rect, Color.WHITE, true)
		draw_rect(eb_rect, Color("0284c7"), false, 1.5)
		draw_string(ThemeDB.fallback_font, eb_rect.position + Vector2(4, 15), emote, HORIZONTAL_ALIGNMENT_CENTER, -1, 10, Color.BLACK)

func _draw_victory_banner(center: Vector2) -> void:
	var font = ThemeDB.fallback_font
	var banner_w = 260.0
	var banner_h = 58.0
	var b_rect = Rect2(center.x - banner_w * 0.5, center.y - banner_h * 0.5, banner_w, banner_h)

	# Royal blue arched ribbon backdrop
	draw_rect(Rect2(b_rect.position.x - 16, b_rect.position.y + 6, banner_w + 32, banner_h - 12), Color("1d4ed8"), true)
	# 3D Golden Banner Face
	draw_rect(b_rect, Color("eab308"), true)
	draw_rect(b_rect, Color("ca8a04"), false, 3.0)

	# "YOU WIN" 3D Text
	draw_string(font, b_rect.position + Vector2(24, 40), "YOU WIN", HORIZONTAL_ALIGNMENT_CENTER, int(banner_w - 48), 32, Color("78350f"))
	draw_string(font, b_rect.position + Vector2(22, 38), "YOU WIN", HORIZONTAL_ALIGNMENT_CENTER, int(banner_w - 48), 32, Color.WHITE)

func _draw_defeat_banner(center: Vector2) -> void:
	var font = ThemeDB.fallback_font
	var banner_w = 260.0
	var banner_h = 58.0
	var b_rect = Rect2(center.x - banner_w * 0.5, center.y - banner_h * 0.5, banner_w, banner_h)

	# Crimson red ribbon
	draw_rect(Rect2(b_rect.position.x - 16, b_rect.position.y + 6, banner_w + 32, banner_h - 12), Color("991b1b"), true)
	draw_rect(b_rect, Color("dc2626"), true)
	draw_rect(b_rect, Color("b91c1c"), false, 3.0)

	draw_string(font, b_rect.position + Vector2(20, 38), "TRY AGAIN", HORIZONTAL_ALIGNMENT_CENTER, int(banner_w - 40), 28, Color.WHITE)

func _draw_interstitial_ad_dialog() -> void:
	draw_rect(Rect2(Vector2.ZERO, size), Color(0, 0, 0, 0.88), true)

	var dialog_w = 460.0
	var dialog_h = 240.0
	var dialog_rect = Rect2((size.x - dialog_w) * 0.5, (size.y - dialog_h) * 0.5, dialog_w, dialog_h)

	draw_rect(dialog_rect, Color("13141e"), true)
	draw_rect(dialog_rect, Color("334155"), false, 2.0)

	var font = ThemeDB.fallback_font

	draw_rect(Rect2(dialog_rect.position + Vector2(16, 16), Vector2(70, 20)), Color("eab308"), true)
	draw_string(font, dialog_rect.position + Vector2(22, 31), "Interstitial Ad", HORIZONTAL_ALIGNMENT_LEFT, -1, 10, Color("0f172a"))

	var skip_label = "Reward Unlocked (X)" if can_skip_ad else "Skip in %ds" % int(ceil(interstitial_timer))
	draw_string(font, dialog_rect.position + Vector2(96, 31), skip_label, HORIZONTAL_ALIGNMENT_LEFT, -1, 11, Color("94a3b8"))

	if can_skip_ad:
		var x_pos = dialog_rect.position + Vector2(dialog_w - 36, 14)
		draw_rect(Rect2(x_pos, Vector2(24, 24)), Color("1e2230"), true)
		draw_string(font, x_pos + Vector2(7, 18), "X", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	var promo_rect = Rect2(dialog_rect.position.x + 20, dialog_rect.position.y + 46, dialog_w - 40, 90)
	draw_rect(promo_rect, Color("1e293b"), true)
	draw_string(font, promo_rect.position + Vector2(16, 30), "CHECKMATE PUZZLE MASTER", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color.WHITE)
	draw_string(font, promo_rect.position + Vector2(16, 52), "Level Solved! Checkmate Delivered.", HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color("4ade80"))
	draw_string(font, promo_rect.position + Vector2(16, 72), "Ad Unit: %s" % ADMOB_INTERSTITIAL_ID, HORIZONTAL_ALIGNMENT_LEFT, -1, 9, Color("64748b"))

	var btn_rect = Rect2(dialog_rect.position.x + (dialog_w - 180) * 0.5, dialog_rect.position.y + 150, 180, 42)
	draw_rect(btn_rect, Color("2563eb"), true)
	draw_string(font, btn_rect.position + Vector2(28, 26), "Next Level >", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color.WHITE)

func _close_interstitial_ad_and_proceed() -> void:
	showing_interstitial_ad = false
	backend.next_level()
	_reset_selection()

func _on_state_changed() -> void:
	queue_redraw()

func _on_move_executed(from_p: Vector2i, to_p: Vector2i, _is_cap: bool) -> void:
	last_move_from = from_p
	last_move_to = to_p
	queue_redraw()

func _on_puzzle_solved(_lvl: int) -> void:
	is_failed = false
	current_player_emote = "GG! ✨"
	current_opponent_emote = "OOPSIE 😭"
	emote_timer = 4.0
	queue_redraw()

func _on_puzzle_failed(_move: String) -> void:
	is_failed = true
	current_player_emote = "OOPSIE 😭"
	current_opponent_emote = "HAHA! 😏"
	emote_timer = 3.0
	queue_redraw()

func _on_interstitial_ad_requested(_ad_unit_id: String) -> void:
	showing_interstitial_ad = true
	interstitial_timer = 2.5
	can_skip_ad = false
	queue_redraw()

func _on_level_changed(_lvl: int, _total: int) -> void:
	last_move_from = Vector2i(-1, -1)
	last_move_to = Vector2i(-1, -1)
	is_failed = false
	_reset_selection()
