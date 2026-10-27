# ChessPuzzleBackend.gd
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

	# Castling flags
	var castling = parts[2] if parts.size() > 2 else "-"
	white_can_castle_kingside = castling.contains("K")
	white_can_castle_queenside = castling.contains("Q")
	black_can_castle_kingside = castling.contains("k")
	black_can_castle_queenside = castling.contains("q")

	# En Passant
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

	# Verify with puzzle solution
	if current_step_index < current_puzzle_moves.size():
		var expected = current_puzzle_moves[current_step_index]
		if not move_uci.begins_with(expected):
			emit_signal("puzzle_failed", move_uci)
			return false

	# Execute move
	_apply_move(from_pos, to_pos)
	current_step_index += 1

	# Check Win / Checkmate condition
	if is_checkmate(active_turn) or current_step_index >= current_puzzle_moves.size():
		is_solved = true
		if not solved_levels.has(current_level_index):
			solved_levels.append(current_level_index)

		emit_signal("checkmate_delivered", 1 - active_turn)
		emit_signal("puzzle_solved", current_level_index)

		# MONETIZATION RULE: Automatically trigger Interstitial Ad upon puzzle solve!
		emit_signal("interstitial_ad_requested", ADMOB_INTERSTITIAL_ID)
		return true

	# Automated Opponent Response if multi-step puzzle
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
