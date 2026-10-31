package com.example.data

import com.example.model.OnlineChallenger
import com.example.model.StrategyLesson

object StrategyRepository {

    val globalChallengersPool = listOf(
        OnlineChallenger("Magnus_Tactics", "Norway", "🇳🇴", 2850, "GM"),
        OnlineChallenger("Hikaru_Speed", "United States", "🇺🇸", 2820, "GM"),
        OnlineChallenger("Wesley_So_Pinoy", "Philippines", "🇵🇭", 2780, "GM"),
        OnlineChallenger("Ding_Dragon", "China", "🇨🇳", 2760, "GM"),
        OnlineChallenger("Anish_Solid", "Netherlands", "🇳🇱", 2750, "GM"),
        OnlineChallenger("Pragg_Future", "India", "🇮🇳", 2740, "GM"),
        OnlineChallenger("Alireza_Attack", "France", "🇫🇷", 2770, "GM"),
        OnlineChallenger("Gukesh_King", "India", "🇮🇳", 2765, "GM"),
        OnlineChallenger("Nakamura_Blitz", "Japan", "🇯🇵", 2690, "IM"),
        OnlineChallenger("Kramnik_Berlin", "Germany", "🇩🇪", 2640, "IM"),
        OnlineChallenger("Kasparov_Legacy", "Azerbaijan", "🇦🇿", 2810, "GM"),
        OnlineChallenger("Fischer_60", "United States", "🇺🇸", 2785, "GM")
    )

    fun getRandomChallenger(excludeName: String = ""): OnlineChallenger {
        val filtered = globalChallengersPool.filter { it.username != excludeName }
        return filtered.randomOrNull() ?: globalChallengersPool.first()
    }

    val lessons: List<StrategyLesson> = listOf(
        StrategyLesson(
            id = "strat_1",
            title = "The Absolute Pin (Taktika ng Gapos)",
            category = "Tactics",
            description = "A pinned piece cannot move because doing so would expose the King to check.",
            keyPrinciple = "Attacking pinned pieces with pawns or minor pieces wins heavy material.",
            exampleFen = "r1bqk2r/pp1n1ppp/2p1pn2/3p4/2PP4/2N1PN2/PP3PPP/R1BQKB1R w KQkq - 0 7",
            keyMove = "c4d5"
        ),
        StrategyLesson(
            id = "strat_2",
            title = "The Deadly Knight Fork (Kabayo Dalawang Target)",
            category = "Tactics",
            description = "The Knight attacks two or more enemy high-value pieces simultaneously without being attacked in return.",
            keyPrinciple = "Look for King and Queen or King and Rook on squares of the same color.",
            exampleFen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 4",
            keyMove = "f3f7"
        ),
        StrategyLesson(
            id = "strat_3",
            title = "Smothered Mate (Suffocated King)",
            category = "Checkmate Patterns",
            description = "The enemy King is completely surrounded and boxed in by his own pieces and executed by a Knight.",
            keyPrinciple = "Sacrifice your Queen to force the enemy Rook to block the King's only escape.",
            exampleFen = "6rk/5Npp/8/8/8/8/8/7K w - - 0 1",
            keyMove = "f7g7"
        ),
        StrategyLesson(
            id = "strat_4",
            title = "Center Control & Rapid Development",
            category = "Opening Strategy",
            description = "Occupy or control the center squares (e4, d4, e5, d5) early with pawns and develop knights and bishops quickly.",
            keyPrinciple = "Do not move the same piece twice in the opening without a tactical reason.",
            exampleFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            keyMove = "e7e5"
        ),
        StrategyLesson(
            id = "strat_5",
            title = "Rook on the 7th Rank (Baboy sa Palayan)",
            category = "Middlegame Strategy",
            description = "A Rook penetrating the 7th rank (or 2nd rank for Black) devours unmoved enemy pawns and paralyzes the enemy King.",
            keyPrinciple = "Doubling rooks on the 7th rank is usually winning in practical endgames.",
            exampleFen = "3r2k1/1R4pp/8/8/8/8/PPP2PPP/3R2K1 w - - 0 1",
            keyMove = "d1d8"
        ),
        StrategyLesson(
            id = "strat_6",
            title = "King & Pawn Endgame: The Opposition",
            category = "Endgame Mastery",
            description = "When kings face each other with one square between them, the player NOT having to move holds the opposition.",
            keyPrinciple = "Step your King forward diagonally to force the enemy King away from defending promotion squares.",
            exampleFen = "8/8/4k3/8/4P3/4K3/8/8 w - - 0 1",
            keyMove = "e3d4"
        )
    )
}
