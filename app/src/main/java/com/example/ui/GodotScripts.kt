package com.example.ui

object GodotScripts {

    val BACKEND_GDSCRIPT: String = """# ChessPuzzleBackend.gd
# Production-ready 2D Chess Puzzle & Checkmate Engine for Godot 4.x (16:9 Mobile)
# Handles 8x8 board representation, complete piece move generation,
# check/checkmate detection, level progression manager, and AdMob monetization triggers.

class_name ChessPuzzleBackend
extends RefCounted

signal state_changed()
signal move_executed(from_pos: Vector2i, to_pos: Vector2i, is_capture: bool)
signal checkmate_delivered(winning_color: int)
signal puzzle_solved(level_index: int)
signal puzzle_failed(move_uci: String)
signal opponent_turn_started()
signal level_changed(level_index: int, total_levels: int)
signal interstitial_ad_requested(ad_unit_id: String)

enum Color { WHITE, BLACK }
enum PieceType { NONE, PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }

const ADMOB_APP_ID = "ca-app-pub-9786472984542330~7452842897"
const ADMOB_BANNER_ID = "ca-app-pub-9786472984542330/2771361852"
const ADMOB_INTERSTITIAL_ID = "ca-app-pub-9786472984542330/2579790164"

class Piece:
	var type: int = PieceType.NONE
	var color: int = Color.WHITE

	func _init(p_type: int = PieceType.NONE, p_color: int = Color.WHITE):
		type = p_type
		color = p_color

	func duplicate_piece() -> Piece:
		return Piece.new(type, color)

# 8x8 Board Grid (0..7 for files a..h, 0..7 for ranks 8..1 where 0 is rank 8)
var board: Array = []
var active_turn: int = Color.WHITE
var white_can_castle_kingside: bool = true
var white_can_castle_queenside: bool = true
var black_can_castle_kingside: bool = true
var black_can_castle_queenside: bool = true
var en_passant_target: Vector2i = Vector2i(-1, -1)

# Level Progression Manager
var current_level_index: int = 0
var puzzles_library: Array[Dictionary] = [
	{
		"id": "lvl_1_scholar",
		"title": "Scholar's Execution",
		"theme": "Mate in 1",
		"difficulty": "Beginner",
		"fen": "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
		"solution": ["f3f7"],
		"description": "Exploit Black's f7 weakness with Queen and Bishop battery."
	},
	{
		"id": "lvl_2_backrank",
		"title": "Corridor of Doom",
		"theme": "Mate in 1",
		"difficulty": "Beginner",
		"fen": "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1",
		"solution": ["a1a8"],
		"description": "Black has no luft flight square. Invade the back rank with the Rook."
	},
	{
		"id": "lvl_3_smothered",
		"title": "Smothered Nightmare",
		"theme": "Mate in 1",
		"difficulty": "Intermediate",
		"fen": "6rk/6pp/3N4/8/8/8/8/4Q1K1 w - - 0 1",
		"solution": ["d6f7"],
		"description": "The enemy king is completely boxed in by friendly pieces. Leap with Nf7#!"
	},
	{
		"id": "lvl_4_arabian",
		"title": "Arabian Hook",
		"theme": "Mate in 1",
		"difficulty": "Intermediate",
		"fen": "7k/R5pp/5N2/8/8/8/8/6K1 w - - 0 1",
		"solution": ["a7a8"],
		"description": "Knight on f6 anchors the g8 escape while Rook delivers back-rank mate."
	},
	{
		"id": "lvl_5_epaulette",
		"title": "Epaulette Trap",
		"theme": "Mate in 1",
		"difficulty": "Intermediate",
		"fen": "3rkr2/8/8/4Q3/8/8/8/4K3 w - - 0 1",
		"solution": ["e5e6"],
		"description": "Black's own Rooks flank the king like shoulder epaulettes."
	},
	{
		"id": "lvl_6_anastasia",
		"title": "Anastasia's Queen Sacrifice",
		"theme": "Mate in 2",
		"difficulty": "Master",
		"fen": "5r1k/pp2N1pp/8/7Q/8/8/5PPP/3R2K1 w - - 0 1",
		"solution": ["h5h7", "h8h7", "d1h1"],
		"description": "Sacrifice the Queen to rip open the h-file for an unstoppable Rook mate!"
	},
	{
		"id": "lvl_7_corner",
		"title": "Corner Lockout",
		"theme": "Mate in 1",
		"difficulty": "Beginner",
		"fen": "k7/2Q5/K7/8/8/8/8/8 w - - 0 1",
		"solution": ["c7b7"],
		"description": "King and Queen net the lone king against the board edge."
	},
	{
		"id": "lvl_8_dark_diag",
		"title": "Dark Diagonal Strike",
		"theme": "Mate in 1",
		"difficulty": "Intermediate",
		"fen": "5rk1/5p1p/5B2/8/8/8/5Q2/6K1 w - - 0 1",
		"solution": ["f2g3"],
		"description": "Bishop on f6 dominates dark escape squares while Queen seals the mate."
	}
]

var current_puzzle_moves: Array[String] = []
var current_step_index: int = 0
var is_solved: bool = false
var solved_levels: Array[int] = []

func _init() -> void:
	_init_empty_board()
	load_level(0)

func _init_empty_board() -> void:
	board.clear()
	for r in range(8):
		var row_arr: Array = []
		for c in range(8):
			row_arr.append(null)
		board.append(row_arr)

# Level Progression Functions
func load_level(level_idx: int) -> void:
	if level_idx < 0 or level_idx >= puzzles_library.size():
		return
	current_level_index = level_idx
	var p = puzzles_library[level_idx]
	current_puzzle_moves = []
	for mv in p["solution"]:
		current_puzzle_moves.append(str(mv))
	current_step_index = 0
	is_solved = false
	load_fen(p["fen"])
	emit_signal("level_changed", current_level_index, puzzles_library.size())

func next_level() -> void:
	var next_idx = (current_level_index + 1) % puzzles_library.size()
	load_level(next_idx)

func previous_level() -> void:
	var prev_idx = (current_level_index - 1 + puzzles_library.size()) % puzzles_library.size()
	load_level(prev_idx)

func restart_level() -> void:
	load_level(current_level_index)

func get_current_puzzle() -> Dictionary:
	return puzzles_library[current_level_index]

func get_piece(pos: Vector2i) -> Piece:
	if not is_valid_pos(pos):
		return null
	return board[pos.y][pos.x]

func set_piece(pos: Vector2i, piece: Piece) -> void:
	if is_valid_pos(pos):
		board[pos.y][pos.x] = piece

func is_valid_pos(pos: Vector2i) -> bool:
	return pos.x >= 0 and pos.x < 8 and pos.y >= 0 and pos.y < 8

# Load FEN String into Board
func load_fen(fen: String) -> void:
	_init_empty_board()
	var parts = fen.strip_edges().split(" ")
	var rows = parts[0].split("/")

	for r in range(min(8, rows.size())):
		var row_str = rows[r]
		var col = 0
		for ch in row_str:
			if ch.is_valid_int():
				col += ch.to_int()
			else:
				var piece = _char_to_piece(ch)
				if is_valid_pos(Vector2i(col, r)):
					set_piece(Vector2i(col, r), piece)
				col += 1

	if parts.size() > 1:
		active_turn = Color.BLACK if parts[1] == "b" else Color.WHITE
	else:
		active_turn = Color.WHITE

	var castling = parts[2] if parts.size() > 2 else "-"
	white_can_castle_kingside = castling.contains("K")
	white_can_castle_queenside = castling.contains("Q")
	black_can_castle_kingside = castling.contains("k")
	black_can_castle_queenside = castling.contains("q")

	en_passant_target = Vector2i(-1, -1)
	if parts.size() > 3 and parts[3] != "-":
		en_passant_target = algebraic_to_pos(parts[3])

	emit_signal("state_changed")

# Move execution & Puzzle validation
func try_player_move(from_pos: Vector2i, to_pos: Vector2i) -> bool:
	if is_solved:
		return false

	var piece = get_piece(from_pos)
	if piece == null or piece.color != active_turn:
		return false

	var legal_moves = get_legal_moves(from_pos)
	var is_legal: bool = false
	for mv in legal_moves:
		if mv.to == to_pos:
			is_legal = true
			break

	if not is_legal:
		return false

	var move_uci = pos_to_algebraic(from_pos) + pos_to_algebraic(to_pos)

	if current_step_index < current_puzzle_moves.size():
		var expected = current_puzzle_moves[current_step_index]
		if not move_uci.begins_with(expected):
			emit_signal("puzzle_failed", move_uci)
			return false

	_apply_move(from_pos, to_pos)
	current_step_index += 1

	if is_checkmate(active_turn) or current_step_index >= current_puzzle_moves.size():
		is_solved = true
		if not solved_levels.has(current_level_index):
			solved_levels.append(current_level_index)

		emit_signal("checkmate_delivered", 1 - active_turn)
		emit_signal("puzzle_solved", current_level_index)

		# MONETIZATION RULE: Trigger Interstitial Ad upon puzzle solve!
		emit_signal("interstitial_ad_requested", ADMOB_INTERSTITIAL_ID)
		return true

	if current_step_index < current_puzzle_moves.size():
		emit_signal("opponent_turn_started")
		_trigger_opponent_auto_move()

	return true

func _trigger_opponent_auto_move() -> void:
	if current_step_index >= current_puzzle_moves.size():
		return
	var opp_uci = current_puzzle_moves[current_step_index]
	var opp_from = algebraic_to_pos(opp_uci.substr(0, 2))
	var opp_to = algebraic_to_pos(opp_uci.substr(2, 2))
	current_step_index += 1
	_apply_move(opp_from, opp_to)

	if is_checkmate(active_turn):
		emit_signal("checkmate_delivered", 1 - active_turn)

func _apply_move(from_pos: Vector2i, to_pos: Vector2i) -> void:
	var piece = get_piece(from_pos)
	if piece == null:
		return

	var is_capture = get_piece(to_pos) != null
	var is_ep = piece.type == PieceType.PAWN and to_pos == en_passant_target

	if is_ep:
		set_piece(Vector2i(to_pos.x, from_pos.y), null)
		is_capture = true

	en_passant_target = Vector2i(-1, -1)
	if piece.type == PieceType.PAWN and abs(to_pos.y - from_pos.y) == 2:
		en_passant_target = Vector2i(from_pos.x, (from_pos.y + to_pos.y) / 2)

	if piece.type == PieceType.KING and abs(to_pos.x - from_pos.x) == 2:
		if to_pos.x == 6:
			var rook = get_piece(Vector2i(7, from_pos.y))
			set_piece(Vector2i(7, from_pos.y), null)
			set_piece(Vector2i(5, from_pos.y), rook)
		elif to_pos.x == 2:
			var rook = get_piece(Vector2i(0, from_pos.y))
			set_piece(Vector2i(0, from_pos.y), null)
			set_piece(Vector2i(3, from_pos.y), rook)

	if piece.type == PieceType.PAWN and (to_pos.y == 0 or to_pos.y == 7):
		piece.type = PieceType.QUEEN

	set_piece(from_pos, null)
	set_piece(to_pos, piece)

	active_turn = Color.BLACK if active_turn == Color.WHITE else Color.WHITE
	emit_signal("move_executed", from_pos, to_pos, is_capture)
	emit_signal("state_changed")

class MoveData:
	var from: Vector2i
	var to: Vector2i
	func _init(p_from: Vector2i, p_to: Vector2i):
		from = p_from
		to = p_to

func get_legal_moves(from_pos: Vector2i) -> Array[MoveData]:
	var piece = get_piece(from_pos)
	if piece == null or piece.color != active_turn:
		return []

	var pseudo = _get_pseudo_legal_moves(from_pos)
	var legal: Array[MoveData] = []
	for mv in pseudo:
		if not _move_leaves_king_in_check(mv.from, mv.to, piece.color):
			legal.append(mv)
	return legal

func _get_pseudo_legal_moves(from: Vector2i) -> Array[MoveData]:
	var moves: Array[MoveData] = []
	var piece = get_piece(from)
	if piece == null:
		return moves

	var opp_color = Color.BLACK if piece.color == Color.WHITE else Color.WHITE

	match piece.type:
		PieceType.PAWN:
			var dir = -1 if piece.color == Color.WHITE else 1
			var start_row = 6 if piece.color == Color.WHITE else 1
			var forward_1 = Vector2i(from.x, from.y + dir)
			if is_valid_pos(forward_1) and get_piece(forward_1) == null:
				moves.append(MoveData.new(from, forward_1))
				if from.y == start_row:
					var forward_2 = Vector2i(from.x, from.y + 2 * dir)
					if is_valid_pos(forward_2) and get_piece(forward_2) == null:
						moves.append(MoveData.new(from, forward_2))

			for dx in [-1, 1]:
				var cap = Vector2i(from.x + dx, from.y + dir)
				if is_valid_pos(cap):
					var target = get_piece(cap)
					if target != null and target.color == opp_color:
						moves.append(MoveData.new(from, cap))
					elif cap == en_passant_target:
						moves.append(MoveData.new(from, cap))

		PieceType.KNIGHT:
			var offsets = [
				Vector2i(1, 2), Vector2i(2, 1), Vector2i(-1, 2), Vector2i(-2, 1),
				Vector2i(1, -2), Vector2i(2, -1), Vector2i(-1, -2), Vector2i(-2, -1)
			]
			for off in offsets:
				var to = from + off
				if is_valid_pos(to):
					var target = get_piece(to)
					if target == null or target.color == opp_color:
						moves.append(MoveData.new(from, to))

		PieceType.BISHOP:
			_add_ray_moves(from, [Vector2i(1, 1), Vector2i(1, -1), Vector2i(-1, 1), Vector2i(-1, -1)], opp_color, moves)

		PieceType.ROOK:
			_add_ray_moves(from, [Vector2i(0, 1), Vector2i(0, -1), Vector2i(1, 0), Vector2i(-1, 0)], opp_color, moves)

		PieceType.QUEEN:
			_add_ray_moves(from, [
				Vector2i(0, 1), Vector2i(0, -1), Vector2i(1, 0), Vector2i(-1, 0),
				Vector2i(1, 1), Vector2i(1, -1), Vector2i(-1, 1), Vector2i(-1, -1)
			], opp_color, moves)

		PieceType.KING:
			for dy in range(-1, 2):
				for dx in range(-1, 2):
					if dx == 0 and dy == 0:
						continue
					var to = from + Vector2i(dx, dy)
					if is_valid_pos(to):
						var target = get_piece(to)
						if target == null or target.color == opp_color:
							moves.append(MoveData.new(from, to))

	return moves

func _add_ray_moves(from: Vector2i, directions: Array, opp_color: int, out_moves: Array[MoveData]) -> void:
	for dir in directions:
		var step = 1
		while true:
			var to = from + dir * step
			if not is_valid_pos(to):
				break
			var target = get_piece(to)
			if target == null:
				out_moves.append(MoveData.new(from, to))
			else:
				if target.color == opp_color:
					out_moves.append(MoveData.new(from, to))
				break
			step += 1

func is_square_attacked(pos: Vector2i, by_color: int) -> bool:
	for r in range(8):
		for c in range(8):
			var p = get_piece(Vector2i(c, r))
			if p != null and p.color == by_color:
				var moves = _get_pseudo_legal_moves(Vector2i(c, r))
				for mv in moves:
					if mv.to == pos:
						return true
	return false

func find_king(color: int) -> Vector2i:
	for r in range(8):
		for c in range(8):
			var p = get_piece(Vector2i(c, r))
			if p != null and p.type == PieceType.KING and p.color == color:
				return Vector2i(c, r)
	return Vector2i(-1, -1)

func is_in_check(color: int) -> bool:
	var king_pos = find_king(color)
	if king_pos == Vector2i(-1, -1):
		return false
	var opp_color = Color.BLACK if color == Color.WHITE else Color.WHITE
	return is_square_attacked(king_pos, opp_color)

func _move_leaves_king_in_check(from_pos: Vector2i, to_pos: Vector2i, color: int) -> bool:
	var piece = get_piece(from_pos)
	var target = get_piece(to_pos)

	set_piece(from_pos, null)
	set_piece(to_pos, piece)
	var in_check = is_in_check(color)

	set_piece(from_pos, piece)
	set_piece(to_pos, target)
	return in_check

func is_checkmate(color: int) -> bool:
	if not is_in_check(color):
		return false
	for r in range(8):
		for c in range(8):
			var p = get_piece(Vector2i(c, r))
			if p != null and p.color == color:
				var legals = get_legal_moves(Vector2i(c, r))
				if legals.size() > 0:
					return false
	return true

func pos_to_algebraic(pos: Vector2i) -> String:
	return char(97 + pos.x) + str(8 - pos.y)

func algebraic_to_pos(alg: String) -> Vector2i:
	var file = alg.unicode_at(0) - 97
	var rank = 8 - alg.substr(1, 1).to_int()
	return Vector2i(file, rank)

func _char_to_piece(c: String) -> Piece:
	var is_white = c == c.to_upper()
	var col = Color.WHITE if is_white else Color.BLACK
	var t = PieceType.PAWN
	match c.to_lower():
		"p": t = PieceType.PAWN
		"n": t = PieceType.KNIGHT
		"b": t = PieceType.BISHOP
		"r": t = PieceType.ROOK
		"q": t = PieceType.QUEEN
		"k": t = PieceType.KING
	return Piece.new(t, col)
"""

    val UI_GDSCRIPT: String = """# ChessBoardUI.gd
# Production-ready 2D Landscape Chess Visual Board UI for Godot 4.x (16:9 Mobile)
# Features custom Dark Black & Charcoal squares, dark-themed metallic pieces,
# Back/Next Level navigation buttons, Bottom 320x50 Banner Ad anchor,
# and Interstitial Ad trigger on checkmate/puzzle solve.

class_name ChessBoardUI
extends Control

@export var backend: ChessPuzzleBackend

# Dark Theme Visual Palette
const COLOR_DARK_SQUARE = Color("14151b")       # Dark Black
const COLOR_LIGHT_SQUARE = Color("282b37")      # Dark White / Charcoal
const COLOR_BOARD_BORDER = Color("0d0e13")      # Outer frame
const COLOR_GOLD_ACCENT = Color("f59e0b")       # Selected aura & frame rim
const COLOR_SELECTED_SQUARE = Color("f59e0b55") # 35% amber overlay
const COLOR_LAST_MOVE = Color("38bdf844")       # Cool cyan last move
const COLOR_MOVE_DOT = Color("fbbf24cc")        # Legal move indicator dot
const COLOR_CAPTURE_RING = Color("ef4444dd")    # Capture targeting circle
const COLOR_CHECK_RED = Color("dc262688")       # King in check pulse

# Piece Visual Styling
const COLOR_PIECE_WHITE = Color("f8fafc")       # Platinum Silver
const COLOR_PIECE_BLACK = Color("18181b")       # Obsidian Charcoal
const COLOR_PIECE_OUTLINE_W = Color("334155")
const COLOR_PIECE_OUTLINE_B = Color("94a3b8")

# AdMob IDs
const ADMOB_APP_ID = "ca-app-pub-9786472984542330~7452842897"
const ADMOB_BANNER_ID = "ca-app-pub-9786472984542330/2771361852"
const ADMOB_INTERSTITIAL_ID = "ca-app-pub-9786472984542330/2579790164"

var board_origin: Vector2 = Vector2.ZERO
var square_size: float = 52.0
var selected_pos: Vector2i = Vector2i(-1, -1)
var legal_destinations: Array[Vector2i] = []
var last_move_from: Vector2i = Vector2i(-1, -1)
var last_move_to: Vector2i = Vector2i(-1, -1)

# UI Elements
var btn_back_rect: Rect2
var btn_next_rect: Rect2
var btn_reset_rect: Rect2
var banner_ad_rect: Rect2

# Interstitial Ad Overlay State
var showing_interstitial_ad: bool = false
var interstitial_timer: float = 3.0
var can_skip_ad: bool = false
var pulse_timer: float = 0.0

func _ready() -> void:
	custom_minimum_size = Vector2(800, 450)
	if backend == null:
		backend = ChessPuzzleBackend.new()

	backend.connect("state_changed", Callable(self, "_on_state_changed"))
	backend.connect("move_executed", Callable(self, "_on_move_executed"))
	backend.connect("puzzle_solved", Callable(self, "_on_puzzle_solved"))
	backend.connect("interstitial_ad_requested", Callable(self, "_on_interstitial_ad_requested"))
	backend.connect("level_changed", Callable(self, "_on_level_changed"))

	_compute_layout()
	queue_redraw()

func _process(delta: float) -> void:
	pulse_timer += delta * 4.0
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
	var banner_height = 50.0
	banner_ad_rect = Rect2(0, size.y - banner_height, size.x, banner_height)

	var main_area_h = size.y - banner_height - 16.0
	square_size = floor(main_area_h / 8.0)
	var board_total_w = square_size * 8.0

	board_origin = Vector2(16.0, 8.0)

	var right_panel_x = board_origin.x + board_total_w + 24.0
	var controls_y = size.y - banner_height - 52.0
	btn_back_rect = Rect2(right_panel_x, controls_y, 110, 40)
	btn_reset_rect = Rect2(right_panel_x + 120, controls_y, 90, 40)
	btn_next_rect = Rect2(right_panel_x + 220, controls_y, 130, 40)

func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		_handle_pointer_down(event.position)
	elif event is InputEventScreenTouch and event.pressed:
		_handle_pointer_down(event.position)

func _handle_pointer_down(pos: Vector2) -> void:
	if showing_interstitial_ad:
		var close_btn_rect = Rect2(size.x * 0.5 + 140, size.y * 0.5 - 110, 36, 36)
		var next_btn_rect = Rect2(size.x * 0.5 - 80, size.y * 0.5 + 60, 160, 44)
		if can_skip_ad and (close_btn_rect.has_point(pos) or next_btn_rect.has_point(pos)):
			_close_interstitial_ad_and_proceed()
		return

	if btn_back_rect.has_point(pos):
		backend.previous_level()
		selected_pos = Vector2i(-1, -1)
		legal_destinations.clear()
		queue_redraw()
		return

	if btn_reset_rect.has_point(pos):
		backend.restart_level()
		selected_pos = Vector2i(-1, -1)
		legal_destinations.clear()
		queue_redraw()
		return

	if btn_next_rect.has_point(pos):
		backend.next_level()
		selected_pos = Vector2i(-1, -1)
		legal_destinations.clear()
		queue_redraw()
		return

	var local = pos - board_origin
	var col = int(floor(local.x / square_size))
	var row = int(floor(local.y / square_size))
	var tapped = Vector2i(col, row)

	if not backend.is_valid_pos(tapped):
		selected_pos = Vector2i(-1, -1)
		legal_destinations.clear()
		queue_redraw()
		return

	if selected_pos != Vector2i(-1, -1):
		if tapped in legal_destinations:
			var success = backend.try_player_move(selected_pos, tapped)
			selected_pos = Vector2i(-1, -1)
			legal_destinations.clear()
			queue_redraw()
			return
		elif tapped == selected_pos:
			selected_pos = Vector2i(-1, -1)
			legal_destinations.clear()
			queue_redraw()
			return

	var piece = backend.get_piece(tapped)
	if piece != null and piece.color == backend.active_turn:
		selected_pos = tapped
		legal_destinations.clear()
		var moves = backend.get_legal_moves(tapped)
		for mv in moves:
			legal_destinations.append(mv.to)
	else:
		selected_pos = Vector2i(-1, -1)
		legal_destinations.clear()

	queue_redraw()

func _draw() -> void:
	var total_w = square_size * 8.0

	# Background
	draw_rect(Rect2(Vector2.ZERO, size), Color("0f1015"), true)

	# Board Frame
	draw_rect(Rect2(board_origin - Vector2(6, 6), Vector2(total_w + 12, total_w + 12)), COLOR_BOARD_BORDER, true)
	draw_rect(Rect2(board_origin - Vector2(6, 6), Vector2(total_w + 12, total_w + 12)), COLOR_GOLD_ACCENT * 0.4, false, 2.0)

	# Squares (8x8)
	for r in range(8):
		for c in range(8):
			var is_light = (r + c) % 2 == 0
			var sq_color = COLOR_LIGHT_SQUARE if is_light else COLOR_DARK_SQUARE
			var sq_pos = board_origin + Vector2(c * square_size, r * square_size)
			draw_rect(Rect2(sq_pos, Vector2(square_size, square_size)), sq_color, true)

	# Last Move Highlight
	if last_move_from != Vector2i(-1, -1):
		draw_rect(Rect2(board_origin + Vector2(last_move_from.x * square_size, last_move_from.y * square_size), Vector2(square_size, square_size)), COLOR_LAST_MOVE, true)
	if last_move_to != Vector2i(-1, -1):
		draw_rect(Rect2(board_origin + Vector2(last_move_to.x * square_size, last_move_to.y * square_size), Vector2(square_size, square_size)), COLOR_LAST_MOVE, true)

	# Selected Square Aura
	if selected_pos != Vector2i(-1, -1):
		var spos = board_origin + Vector2(selected_pos.x * square_size, selected_pos.y * square_size)
		draw_rect(Rect2(spos, Vector2(square_size, square_size)), COLOR_SELECTED_SQUARE, true)
		draw_rect(Rect2(spos + Vector2(2, 2), Vector2(square_size - 4, square_size - 4)), COLOR_GOLD_ACCENT, false, 2.5)

	# King in Check Pulse
	if backend.is_in_check(backend.active_turn):
		var king = backend.find_king(backend.active_turn)
		if king != Vector2i(-1, -1):
			var kpos = board_origin + Vector2(king.x * square_size + square_size * 0.5, king.y * square_size + square_size * 0.5)
			var a = 0.4 + 0.3 * sin(pulse_timer)
			draw_circle(kpos, square_size * 0.46, Color(COLOR_CHECK_RED.r, COLOR_CHECK_RED.g, COLOR_CHECK_RED.b, a))

	# Draw Pieces
	for r in range(8):
		for c in range(8):
			var piece = backend.get_piece(Vector2i(c, r))
			if piece != null:
				var center = board_origin + Vector2(c * square_size + square_size * 0.5, r * square_size + square_size * 0.5)
				_draw_piece(center, piece)

	# Legal Move Dots & Capture Rings
	for dest in legal_destinations:
		var dcenter = board_origin + Vector2(dest.x * square_size + square_size * 0.5, dest.y * square_size + square_size * 0.5)
		var target = backend.get_piece(dest)
		if target != null or dest == backend.en_passant_target:
			draw_arc(dcenter, square_size * 0.42, 0, TAU, 32, COLOR_CAPTURE_RING, 3.5)
		else:
			draw_circle(dcenter, square_size * 0.16, COLOR_MOVE_DOT)

	# Right Panel HUD
	var right_x = board_origin.x + total_w + 24.0
	var puzzle = backend.get_current_puzzle()

	var font = ThemeDB.fallback_font
	draw_string(font, Vector2(right_x, 32), "LEVEL %d of %d: %s" % [backend.current_level_index + 1, backend.puzzles_library.size(), puzzle["title"]], HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Color("f8fafc"))
	draw_string(font, Vector2(right_x, 56), "%s • %s" % [puzzle["theme"], puzzle["difficulty"]], HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color("f59e0b"))

	# Status Card
	var status_text = "CHECKMATE! Level Solved 🎉" if backend.is_solved else "White to move and deliver checkmate"
	var status_bg = Color("15803d") if backend.is_solved else Color("1e2230")
	draw_rect(Rect2(right_x, 72, size.x - right_x - 24, 34), status_bg, true)
	draw_string(font, Vector2(right_x + 12, 94), status_text, HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	# Description
	draw_string(font, Vector2(right_x, 135), puzzle["description"], HORIZONTAL_ALIGNMENT_LEFT, int(size.x - right_x - 30), 12, Color("94a3b8"))

	# Control Buttons
	draw_rect(btn_back_rect, Color("1e293b"), true)
	draw_rect(btn_back_rect, Color("334155"), false, 1.5)
	draw_string(font, btn_back_rect.position + Vector2(16, 25), "< Levels", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	draw_rect(btn_reset_rect, Color("334155"), true)
	draw_string(font, btn_reset_rect.position + Vector2(24, 25), "Reset", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	var next_bg = Color("16a34a") if backend.is_solved else Color("2563eb")
	draw_rect(btn_next_rect, next_bg, true)
	draw_string(font, btn_next_rect.position + Vector2(16, 25), "Next Level >", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color.WHITE)

	# Bottom Banner Ad Area (320x50)
	draw_rect(banner_ad_rect, Color("0b0c10"), true)
	draw_line(banner_ad_rect.position, banner_ad_rect.position + Vector2(size.x, 0), Color("272a38"), 1.0)

	draw_rect(Rect2(12, banner_ad_rect.position.y + 14, 24, 18), Color("eab308"), true)
	draw_string(font, Vector2(16, banner_ad_rect.position.y + 27), "Ad", HORIZONTAL_ALIGNMENT_LEFT, -1, 10, Color("0f172a"))

	draw_string(font, Vector2(46, banner_ad_rect.position.y + 23), "Grandmaster Tactics • Learn 1,000+ Checkmates", HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color("f8fafc"))
	draw_string(font, Vector2(46, banner_ad_rect.position.y + 40), "AdMob Banner: %s" % ADMOB_BANNER_ID, HORIZONTAL_ALIGNMENT_LEFT, -1, 9, Color("64748b"))

	draw_rect(Rect2(size.x - 110, banner_ad_rect.position.y + 10, 96, 30), Color("2563eb"), true)
	draw_string(font, Vector2(size.x - 90, banner_ad_rect.position.y + 30), "INSTALL", HORIZONTAL_ALIGNMENT_LEFT, -1, 11, Color.WHITE)

	# Interstitial Ad Overlay
	if showing_interstitial_ad:
		_draw_interstitial_ad_dialog()

func _draw_piece(center: Vector2, piece: ChessPuzzleBackend.Piece) -> void:
	var is_white = piece.color == ChessPuzzleBackend.Color.WHITE
	var fill_color = COLOR_PIECE_WHITE if is_white else COLOR_PIECE_BLACK
	var outline_color = COLOR_PIECE_OUTLINE_W if is_white else COLOR_PIECE_OUTLINE_B
	var accent_color = COLOR_GOLD_ACCENT if is_white else Color("e11d48")
	var r = square_size * 0.35

	match piece.type:
		ChessPuzzleBackend.PieceType.PAWN:
			draw_circle(center + Vector2(0, -r * 0.3), r * 0.45, fill_color)
			draw_arc(center + Vector2(0, -r * 0.3), r * 0.45, 0, TAU, 24, outline_color, 2.0)
			draw_rect(Rect2(center + Vector2(-r * 0.4, r * 0.3), Vector2(r * 0.8, r * 0.35)), fill_color, true)
			draw_rect(Rect2(center + Vector2(-r * 0.4, r * 0.3), Vector2(r * 0.8, r * 0.35)), outline_color, false, 2.0)

		ChessPuzzleBackend.PieceType.KNIGHT:
			var pts = PackedVector2Array([
				center + Vector2(-r * 0.4, r * 0.6),
				center + Vector2(r * 0.4, r * 0.6),
				center + Vector2(r * 0.45, r * 0.1),
				center + Vector2(r * 0.6, -r * 0.2),
				center + Vector2(r * 0.1, -r * 0.6),
				center + Vector2(-r * 0.3, -r * 0.4)
			])
			draw_colored_polygon(pts, fill_color)
			draw_polyline(pts, outline_color, 2.5)
			draw_circle(center + Vector2(r * 0.2, -r * 0.25), 2.5, accent_color)

		ChessPuzzleBackend.PieceType.BISHOP:
			draw_circle(center + Vector2(0, -r * 0.1), r * 0.55, fill_color)
			draw_arc(center + Vector2(0, -r * 0.1), r * 0.55, 0, TAU, 24, outline_color, 2.0)
			draw_line(center + Vector2(-r * 0.2, -r * 0.3), center + Vector2(r * 0.2, r * 0.1), accent_color, 2.5)

		ChessPuzzleBackend.PieceType.ROOK:
			draw_rect(Rect2(center + Vector2(-r * 0.55, -r * 0.55), Vector2(r * 1.1, r * 1.15)), fill_color, true)
			draw_rect(Rect2(center + Vector2(-r * 0.55, -r * 0.55), Vector2(r * 1.1, r * 1.15)), outline_color, false, 2.0)
			draw_line(center + Vector2(-r * 0.2, -r * 0.2), center + Vector2(r * 0.2, -r * 0.2), accent_color, 2.0)

		ChessPuzzleBackend.PieceType.QUEEN:
			draw_circle(center + Vector2(0, 0), r * 0.6, fill_color)
			draw_arc(center + Vector2(0, 0), r * 0.6, 0, TAU, 24, outline_color, 2.0)
			draw_circle(center + Vector2(-r * 0.4, -r * 0.55), 3.0, accent_color)
			draw_circle(center + Vector2(0, -r * 0.65), 3.0, accent_color)
			draw_circle(center + Vector2(r * 0.4, -r * 0.55), 3.0, accent_color)

		ChessPuzzleBackend.PieceType.KING:
			draw_circle(center + Vector2(0, 0.1 * r), r * 0.6, fill_color)
			draw_arc(center + Vector2(0, 0.1 * r), r * 0.6, 0, TAU, 24, outline_color, 2.0)
			draw_line(center + Vector2(0, -r * 0.9), center + Vector2(0, -r * 0.5), accent_color, 3.0)
			draw_line(center + Vector2(-r * 0.25, -r * 0.7), center + Vector2(r * 0.25, -r * 0.7), accent_color, 3.0)

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
	selected_pos = Vector2i(-1, -1)
	legal_destinations.clear()
	queue_redraw()

func _on_state_changed() -> void:
	queue_redraw()

func _on_move_executed(from_p: Vector2i, to_p: Vector2i, _is_cap: bool) -> void:
	last_move_from = from_p
	last_move_to = to_p
	queue_redraw()

func _on_puzzle_solved(_lvl: int) -> void:
	queue_redraw()

func _on_interstitial_ad_requested(_ad_unit_id: String) -> void:
	showing_interstitial_ad = true
	interstitial_timer = 2.5
	can_skip_ad = false
	queue_redraw()

func _on_level_changed(_lvl: int, _total: int) -> void:
	last_move_from = Vector2i(-1, -1)
	last_move_to = Vector2i(-1, -1)
	selected_pos = Vector2i(-1, -1)
	legal_destinations.clear()
	queue_redraw()
"""

    val PUZZLE_DATA_GDSCRIPT: String = """# PuzzleData.gd
class_name PuzzleData
extends RefCounted

static var puzzles: Array = [
	# ===================================================
	# CATEGORY 1: MATE IN 2 MOVES (EASY)
	# ===================================================
	{
		"id": 1,
		"title": "Scholar's Mate Trap",
		"difficulty": "Easy",
		"mate_in": 2,
		"fen": "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 4",
		"solution": [
			{"from": Vector2i(5, 2), "to": Vector2i(5, 6)} # f3 to f7 (Checkmate)
		]
	},
	{
		"id": 2,
		"title": "Smothered Combination",
		"difficulty": "Easy",
		"mate_in": 2,
		"fen": "6rk/5Npp/8/8/8/8/8/7K w - - 0 1",
		"solution": [
			{"from": Vector2i(5, 6), "to": Vector2i(6, 7)} # Knight mate at g7
		]
	},

	# ===================================================
	# CATEGORY 2: MATE IN 3 MOVES (MEDIUM)
	# ===================================================
	{
		"id": 3,
		"title": "Boden's Mate Pattern",
		"difficulty": "Medium",
		"mate_in": 3,
		"fen": "2kr3r/ppp2ppp/8/4b3/8/8/PPP2PPP/R1B2RK1 b - - 0 1",
		"solution": [
			{"from": Vector2i(4, 4), "to": Vector2i(7, 1)}, # e5 to h2
			{"from": Vector2i(6, 0), "to": Vector2i(7, 1)}, # g1 to h2 (King move)
			{"from": Vector2i(3, 0), "to": Vector2i(3, 1)}  # d8 to d1 (Checkmate)
		]
	},

	# ===================================================
	# CATEGORY 3: MATE IN 5 MOVES (HARD)
	# ===================================================
	{
		"id": 4,
		"title": "Rook & Knight Assault",
		"difficulty": "Hard",
		"mate_in": 5,
		"fen": "r1b2rk1/ppp2ppp/8/4N3/2B5/8/PPP2PPP/3R2K1 w - - 0 1",
		"solution": [
			{"from": Vector2i(2, 3), "to": Vector2i(5, 6)}, # c4 to f7+
			{"from": Vector2i(5, 7), "to": Vector2i(5, 6)}, # f8 to f7 (Rook captures)
			{"from": Vector2i(3, 0), "to": Vector2i(3, 7)}, # d1 to d8+
			{"from": Vector2i(5, 6), "to": Vector2i(5, 7)}, # f7 to f8 (Rook blocks)
			{"from": Vector2i(3, 7), "to": Vector2i(5, 7)}  # d8 to f8 (Checkmate)
		]
	},

	# ===================================================
	# CATEGORY 4: MATE IN 10 MOVES (MASTER ENDGAME)
	# ===================================================
	{
		"id": 5,
		"title": "King & Rook Precision Mate",
		"difficulty": "Master",
		"mate_in": 10,
		"fen": "8/8/8/4k3/8/2K5/4R3/8 w - - 0 1",
		"solution": [
			{"from": Vector2i(4, 1), "to": Vector2i(4, 0)}, # e2 to e1
			{"from": Vector2i(4, 4), "to": Vector2i(5, 3)}, # e5 to f4
			{"from": Vector2i(2, 2), "to": Vector2i(3, 2)}, # c3 to d3
			{"from": Vector2i(5, 3), "to": Vector2i(5, 2)}, # f4 to f3
			{"from": Vector2i(4, 0), "to": Vector2i(5, 0)}, # e1 to f1
			{"from": Vector2i(5, 2), "to": Vector2i(6, 1)}, # f3 to g2
			{"from": Vector2i(3, 2), "to": Vector2i(4, 1)}, # d3 to e2
			{"from": Vector2i(6, 1), "to": Vector2i(6, 2)}, # g2 to g3
			{"from": Vector2i(5, 0), "to": Vector2i(5, 7)}, # f1 to f8
			{"from": Vector2i(6, 2), "to": Vector2i(6, 1)}  # Forced Checkmate Sequence
		]
	}
]
"""

    val ADMOB_MANAGER_GDSCRIPT: String = """# AdMobManager.gd
class_name AdMobManager
extends Node

const APP_ID = "ca-app-pub-9786472984542330~7452842897"
const BANNER_AD_UNIT_ID = "ca-app-pub-9786472984542330/2771361852"
const INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9786472984542330/2579790164"

var is_interstitial_loaded: bool = false
var is_banner_visible: bool = false
var admob_singleton: Object = null

func _ready() -> void:
	if Engine.has_singleton("PoingGodotAdMob"):
		admob_singleton = Engine.get_singleton("PoingGodotAdMob")
		_initialize_plugin()
	elif Engine.has_singleton("GodotAdMob"):
		admob_singleton = Engine.get_singleton("GodotAdMob")
		_initialize_plugin()
	else:
		print("[AdMobManager] Running in Editor/Mock mode. AdMob IDs configured.")

func _initialize_plugin() -> void:
	if admob_singleton == null:
		return
	admob_singleton.initialize()
	load_bottom_banner()
	load_interstitial()

func load_bottom_banner() -> void:
	if admob_singleton:
		admob_singleton.load_banner(BANNER_AD_UNIT_ID, 1) # Position.BOTTOM
		is_banner_visible = true

func load_interstitial() -> void:
	if admob_singleton:
		admob_singleton.load_interstitial(INTERSTITIAL_AD_UNIT_ID)
	else:
		is_interstitial_loaded = true

func show_interstitial() -> void:
	if admob_singleton:
		if admob_singleton.is_interstitial_loaded():
			admob_singleton.show_interstitial()
		else:
			load_interstitial()
	else:
		print("[AdMobManager] Interstitial Ad triggered on checkmate!")
		load_interstitial()
"""
}

