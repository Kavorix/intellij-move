#[test]
module 0x1::M {
    #[test_only]
    use 0x1::SomeOtherModule;

    #[test_only]
    const MY_CONST: u8 = 1;

    #[test_only]
    struct SomeStruct {}

    #[test_only]
    native struct S;

    #[test]
    #[expected_failure]
    fun main() {}

    #[attr1, attr2]
    fun main2() {}

    #[test(a = @0x1, b = @0x2, c = @Std)]
    fun main3() {}

    #[test_only]
    native fun native_main();

    #[show(book_orders_sdk, book_price_levels_sdk)]
    fun test() {}

    #[expected_failure(abort_code = liquidswap::liquidity_pool::ERR_ADMIN)]
    fun abort_test() {}

    #[expected_failure(
        abort_code = liquidity_pool::ERR_ADMIN,
        location = aptos_framework::ed25519,
        location = aptos_framework::ed25519::myfunction,
    )]
    fun abort_test_2() {}
}
