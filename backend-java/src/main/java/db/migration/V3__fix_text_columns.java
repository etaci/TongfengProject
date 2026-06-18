package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__fix_text_columns extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		String databaseProductName = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
		String textType = resolveTextType(databaseProductName);

		alterColumn(connection, "lab_report_record", "indicators_json", textType, false);
		alterColumn(connection, "lab_report_record", "suggestions_json", textType, false);
		alterColumn(connection, "meal_record", "items_json", textType, false);
		alterColumn(connection, "meal_record", "suggestions_json", textType, false);
		alterColumn(connection, "medication_plan", "current_medications_json", textType, false);
		alterColumn(connection, "mvp_usage_event", "payload_json", textType, false);
		alterColumn(connection, "user_profile", "allergies_json", textType, true);
		alterColumn(connection, "user_profile", "comorbidities_json", textType, true);
	}

	private String resolveTextType(String databaseProductName) {
		if (databaseProductName.contains("h2")) {
			return "CLOB";
		}
		if (databaseProductName.contains("mysql")) {
			return "LONGTEXT";
		}
		throw new IllegalStateException("Unsupported database for V3__fix_text_columns: " + databaseProductName);
	}

	private void alterColumn(Connection connection, String tableName, String columnName, String textType, boolean nullable)
			throws SQLException {
		String nullability = nullable ? "NULL" : "NOT NULL";
		String sql = "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + textType + " " + nullability;
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}
}
