package pe.casaconectada.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceTest {
    @Test fun pendingContributionsDoNotIncreaseConfirmedBalance() {
        val contributions = listOf(
            Contribution("1", "g1", "m1", 2500, "PENDIENTE"),
            Contribution("2", "g1", "m1", 10000, "CONFIRMADO"),
            Contribution("3", "g2", "m1", 3000, "CONFIRMADO"),
        )
        assertEquals(10000L, confirmedBalance(contributions, "g1"))
    }
}
