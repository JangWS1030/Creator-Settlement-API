package io.github.jangws1030.creatorsettlementapi.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseObjectInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseObjectInitializer.class);

    @Bean
    public ApplicationRunner databaseObjectRunner(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            if (!isMysql(dataSource)) {
                return;
            }

            List<String> dropStatements = List.of(
                    "drop view if exists vw_creator_monthly_settlement_base",
                    "drop view if exists vw_sale_audit",
                    "drop function if exists fn_scheduled_settlement_amount",
                    "drop function if exists fn_platform_fee_amount",
                    "drop function if exists fn_net_sale_amount"
            );

            List<String> createStatements = List.of(
                    """
                    create function fn_net_sale_amount(total_sale_amount bigint, refund_amount bigint)
                    returns bigint
                    deterministic
                    return ifnull(total_sale_amount, 0) - ifnull(refund_amount, 0)
                    """,
                    """
                    create function fn_platform_fee_amount(net_sale_amount bigint, fee_rate_percentage int)
                    returns bigint
                    deterministic
                    return cast(round((ifnull(net_sale_amount, 0) * ifnull(fee_rate_percentage, 0)) / 100, 0) as signed)
                    """,
                    """
                    create function fn_scheduled_settlement_amount(total_sale_amount bigint, refund_amount bigint, fee_rate_percentage int)
                    returns bigint
                    deterministic
                    return fn_net_sale_amount(total_sale_amount, refund_amount)
                           - fn_platform_fee_amount(fn_net_sale_amount(total_sale_amount, refund_amount), fee_rate_percentage)
                    """,
                    """
                    create view vw_sale_audit as
                    select
                        s.id as sale_id,
                        cr.id as creator_id,
                        cr.name as creator_name,
                        co.id as course_id,
                        co.title as course_title,
                        s.student_id,
                        s.amount as sale_amount,
                        convert_tz(s.paid_at, '+00:00', '+09:00') as paid_at,
                        ifnull(cancel_summary.refunded_amount, 0) as refunded_amount,
                        fn_net_sale_amount(s.amount, ifnull(cancel_summary.refunded_amount, 0)) as remaining_amount,
                        ifnull(cancel_summary.cancellation_count, 0) as cancellation_count,
                        convert_tz(cancel_summary.last_cancelled_at, '+00:00', '+09:00') as last_cancelled_at,
                        case
                            when ifnull(cancel_summary.refunded_amount, 0) = 0 then 'COMPLETED'
                            when ifnull(cancel_summary.refunded_amount, 0) >= s.amount then 'FULLY_REFUNDED'
                            else 'PARTIALLY_REFUNDED'
                        end as sale_status
                    from sale_records s
                    join courses co on co.id = s.course_id
                    join creators cr on cr.id = co.creator_id
                    left join (
                        select
                            sale_record_id,
                            sum(refund_amount) as refunded_amount,
                            count(*) as cancellation_count,
                            max(cancelled_at) as last_cancelled_at
                        from cancellation_records
                        group by sale_record_id
                    ) cancel_summary on cancel_summary.sale_record_id = s.id
                    """,
                    """
                    create view vw_creator_monthly_settlement_base as
                    select
                        month_base.creator_id,
                        month_base.creator_name,
                        month_base.settlement_month,
                        ifnull(sales.total_sale_amount, 0) as total_sale_amount,
                        ifnull(refunds.refund_amount, 0) as refund_amount,
                        fn_net_sale_amount(ifnull(sales.total_sale_amount, 0), ifnull(refunds.refund_amount, 0)) as net_sale_amount,
                        ifnull(sales.sale_count, 0) as sale_count,
                        ifnull(refunds.cancellation_count, 0) as cancellation_count
                    from (
                        select
                            c.id as creator_id,
                            c.name as creator_name,
                            date_format(convert_tz(s.paid_at, '+00:00', '+09:00'), '%Y-%m') as settlement_month
                        from sale_records s
                        join courses co on co.id = s.course_id
                        join creators c on c.id = co.creator_id
                        group by c.id, c.name, date_format(convert_tz(s.paid_at, '+00:00', '+09:00'), '%Y-%m')
                        union
                        select
                            c.id as creator_id,
                            c.name as creator_name,
                            date_format(convert_tz(cr.cancelled_at, '+00:00', '+09:00'), '%Y-%m') as settlement_month
                        from cancellation_records cr
                        join sale_records s on s.id = cr.sale_record_id
                        join courses co on co.id = s.course_id
                        join creators c on c.id = co.creator_id
                        group by c.id, c.name, date_format(convert_tz(cr.cancelled_at, '+00:00', '+09:00'), '%Y-%m')
                    ) month_base
                    left join (
                        select
                            c.id as creator_id,
                            date_format(convert_tz(s.paid_at, '+00:00', '+09:00'), '%Y-%m') as settlement_month,
                            sum(s.amount) as total_sale_amount,
                            count(*) as sale_count
                        from sale_records s
                        join courses co on co.id = s.course_id
                        join creators c on c.id = co.creator_id
                        group by c.id, date_format(convert_tz(s.paid_at, '+00:00', '+09:00'), '%Y-%m')
                    ) sales on sales.creator_id = month_base.creator_id
                           and sales.settlement_month = month_base.settlement_month
                    left join (
                        select
                            c.id as creator_id,
                            date_format(convert_tz(cr.cancelled_at, '+00:00', '+09:00'), '%Y-%m') as settlement_month,
                            sum(cr.refund_amount) as refund_amount,
                            count(*) as cancellation_count
                        from cancellation_records cr
                        join sale_records s on s.id = cr.sale_record_id
                        join courses co on co.id = s.course_id
                        join creators c on c.id = co.creator_id
                        group by c.id, date_format(convert_tz(cr.cancelled_at, '+00:00', '+09:00'), '%Y-%m')
                    ) refunds on refunds.creator_id = month_base.creator_id
                             and refunds.settlement_month = month_base.settlement_month
                    """
            );

            for (String statement : dropStatements) {
                jdbcTemplate.execute(statement);
            }

            for (String statement : createStatements) {
                jdbcTemplate.execute(statement);
            }

            log.info("MySQL reporting views/functions initialized.");
        };
    }

    private boolean isMysql(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql");
        }
    }
}
