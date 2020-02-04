package com.sorda.jfx

import com.sorda.states.BidState

data class BidData(val bidState: BidState, val bidStatus: BidStatus)

enum class BidStatus {
    WINNING,
    OUTBID,
    WON,
    LOST,
    UNKNOWN
}