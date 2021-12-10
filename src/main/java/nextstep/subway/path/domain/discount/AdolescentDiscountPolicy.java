package nextstep.subway.path.domain.discount;

public class AdolescentDiscountPolicy implements DiscountPolicy{

    private static final int DEDUCTIBLE_FEE = 350;
    private static final double DISCOUNT_RETE = 0.2;
    private static final int ZERO_FEE = 0;

    @Override
    public int discount(int fee) {
        fee -= DEDUCTIBLE_FEE;
        if (fee  <= ZERO_FEE) {
            return ZERO_FEE;
        }
        return calculateDiscountFee(fee, DISCOUNT_RETE);
    }
}
