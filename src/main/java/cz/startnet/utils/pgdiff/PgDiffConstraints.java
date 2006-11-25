/*
 * $CVSHeader$
 */
package cz.startnet.utils.pgdiff;

import cz.startnet.utils.pgdiff.schema.PgConstraint;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgTable;

import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Diffs constraints.
 *
 * @author fordfrog
 * @version $CVSHeader$
 */
public class PgDiffConstraints {
    /**
     * Creates a new instance of PgDiffConstraints.
     */
    private PgDiffConstraints() {
        super();
    }

    /**
     * Outputs commands for differences in constraints.
     *
     * @param writer writer the output should be written to
     * @param schema1 original schema
     * @param schema2 new schema
     * @param primaryKey determines whether primery keys should be processed or
     *        any other constraints should be processed
     */
    public static void diffConstraints(
        final PrintWriter writer,
        final PgSchema schema1,
        final PgSchema schema2,
        final boolean primaryKey) {
        final Map<String, PgTable> tables1 = schema1.getTables();

        for (PgTable table2 : schema2.getTables().values()) {
            final String tableName2 = table2.getName();
            final PgTable table1 = tables1.get(tableName2);

            // Drop constraints that do not exist in new schema or are modified
            for (PgConstraint constraint : getDropConstraints(
                        tables1.get(tableName2),
                        table2,
                        primaryKey)) {
                writer.println();
                writer.println("ALTER TABLE " + tableName2);
                writer.println(
                        "\tDROP CONSTRAINT " + constraint.getName() + ";");
            }

            // Add new constraints
            for (PgConstraint constraint : getNewConstraints(
                        tables1.get(tableName2),
                        table2,
                        primaryKey)) {
                writer.println();
                writer.println("ALTER TABLE " + tableName2);
                writer.println(
                        "\tADD CONSTRAINT " + constraint.getName() + " "
                        + constraint.getDefinition() + ";");
            }

            if ((table1 != null) && !primaryKey) {
                dropOrCreateCluster(writer, tables1.get(tableName2), table2);
            }
        }
    }

    /**
     * Returns list of constraints that should be dropped.
     *
     * @param table1 original table or null
     * @param table2 new table or null
     * @param primaryKey determines whether primery keys should be processed or
     *        any other constraints should be processed
     *
     * @return list of constraints that should be dropped
     *
     * @todo Constraints that are dependent on a removed field should not be
     *       added to drop because they are already removed.
     */
    private static List<PgConstraint> getDropConstraints(
        final PgTable table1,
        final PgTable table2,
        final boolean primaryKey) {
        final List<PgConstraint> list = new ArrayList<PgConstraint>();

        if ((table2 != null) && (table1 != null)) {
            final Set<String> names2 = table2.getConstraints().keySet();

            for (final PgConstraint constraint : table1.getConstraints().values()) {
                if (
                    (constraint.isPrimaryKeyConstraint() == primaryKey)
                        && (!names2.contains(constraint.getName())
                        || !table2.getConstraint(constraint.getName())
                                      .getDefinition().equals(
                                constraint.getDefinition()))) {
                    list.add(constraint);
                }
            }
        }

        return list;
    }

    /**
     * Returns list of constraints that should be added.
     *
     * @param table1 original table
     * @param table2 new table
     * @param primaryKey determines whether primery keys should be processed or
     *        any other constraints should be processed
     *
     * @return list of constraints that should be added
     */
    private static List<PgConstraint> getNewConstraints(
        final PgTable table1,
        final PgTable table2,
        final boolean primaryKey) {
        final List<PgConstraint> list = new ArrayList<PgConstraint>();

        if (table2 != null) {
            if (table1 == null) {
                for (final PgConstraint constraint : table2.getConstraints()
                                                           .values()) {
                    if (constraint.isPrimaryKeyConstraint() == primaryKey) {
                        list.add(constraint);
                    }
                }
            } else {
                final Set<String> names1 = table1.getConstraints().keySet();

                for (final PgConstraint constraint : table2.getConstraints()
                                                           .values()) {
                    if (
                        (constraint.isPrimaryKeyConstraint() == primaryKey)
                            && (!names1.contains(constraint.getName())
                            || !table1.getConstraint(constraint.getName())
                                          .getDefinition().equals(
                                    constraint.getDefinition()))) {
                        list.add(constraint);
                    }
                }
            }
        }

        return list;
    }

    /**
     * Generates and outputs CLUSTER specific DDL if appropriate.
     *
     * @param writer writer the output should be written to
     * @param table1 original table
     * @param table2 new table
     */
    private static void dropOrCreateCluster(
        final PrintWriter writer,
        final PgTable table1,
        final PgTable table2) {
        final String oldCluster = table1.getClusterIndexName();
        final String newCluster = table2.getClusterIndexName();

        if ((oldCluster == null) && (newCluster != null)) {
            writer.println();
            writer.print("ALTER TABLE ");
            writer.print(table2.getName());
            writer.print(" CLUSTER ON ");
            writer.print(newCluster);
            writer.print(" ;");
        } else if ((oldCluster != null) && (newCluster == null)) {
            writer.println();
            writer.print("ALTER TABLE ");
            writer.print(table2.getName());
            writer.print(" SET WITHOUT CLUSTER;");
        } else if (
            (oldCluster != null)
                && (newCluster != null)
                && (newCluster.compareTo(oldCluster) != 0)) {
            writer.println();
            writer.print("ALTER TABLE ");
            writer.print(table2.getName());
            writer.print(" CLUSTER ON ");
            writer.print(newCluster);
            writer.print(" ;");
        }
    }
}