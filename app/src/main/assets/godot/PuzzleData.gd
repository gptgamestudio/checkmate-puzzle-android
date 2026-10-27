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
