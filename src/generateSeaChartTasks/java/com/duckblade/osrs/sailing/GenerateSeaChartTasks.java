package com.duckblade.osrs.sailing;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class GenerateSeaChartTasks
{

	private static final String VARBIT_ID_URL = "https://github.com/runelite/runelite/raw/refs/heads/master/runelite-api/src/main/java/net/runelite/api/gameval/VarbitID.java";
	private static final String OBJECT_ID_URL = "https://github.com/runelite/runelite/raw/refs/heads/master/runelite-api/src/main/java/net/runelite/api/gameval/ObjectID.java";
	private static final String OBJECT_ID_1_URL = "https://github.com/runelite/runelite/raw/refs/heads/master/runelite-api/src/main/java/net/runelite/api/gameval/ObjectID1.java";
	private static final String NPC_ID_URL = "https://github.com/runelite/runelite/raw/refs/heads/master/runelite-api/src/main/java/net/runelite/api/gameval/NpcID.java";

	public static void main(String[] main) throws Exception
	{
		Map<Integer, String> varbNames = readConstants(VARBIT_ID_URL);
		Map<Integer, String> objectNames = readConstants(OBJECT_ID_URL);
		objectNames.putAll(readConstants(OBJECT_ID_1_URL));
		Map<Integer, String> npcNames = readConstants(NPC_ID_URL);
		System.setOut(new PrintStream(new FileOutputStream("src/main/java/com/duckblade/osrs/sailing/features/charting/SeaChartTask.java")));

		System.out.println("package com.duckblade.osrs.sailing.features.charting;");
		System.out.println();
		System.out.println("import lombok.Getter;");
		System.out.println("import lombok.RequiredArgsConstructor;");
		System.out.println("import net.runelite.api.Client;");
		System.out.println("import net.runelite.api.coords.WorldPoint;");
		System.out.println("import net.runelite.api.gameval.VarbitID;");
		System.out.println("import net.runelite.api.gameval.ObjectID;");
		System.out.println("import net.runelite.api.gameval.NpcID;");
		System.out.println();
		System.out.println("@RequiredArgsConstructor");
		System.out.println("@Getter");
		System.out.println("public enum SeaChartTask");
		System.out.println("{");
		System.out.println();

		try (Scanner scanner = new Scanner(Objects.requireNonNull(GenerateSeaChartTasks.class.getResourceAsStream("/chartables.tsv"))))
		{
			scanner.nextLine(); // skip header
			while (scanner.hasNextLine())
			{
				String[] parts = scanner.nextLine().split("\t");
				int taskId = Integer.parseInt(parts[0]);
				int varb = Integer.parseInt(parts[1]);
				int object = Integer.parseInt(parts[2]);
				int npc = Integer.parseInt(parts[3]);
				int x = Integer.parseInt(parts[4]);
				int y = Integer.parseInt(parts[5]);
				int dstX = Integer.parseInt(parts[6]);
				int dstY = Integer.parseInt(parts[7]);
				int level = Integer.parseInt(parts[8]);

				System.out.print("\t");
				System.out.print("TASK_");
				System.out.print(taskId);
				System.out.print("(");
				System.out.print(taskId);
				System.out.print(", ");
				System.out.print("VarbitID." + varbNames.get(varb));
				System.out.print(", ");
				if (object != -1)
				{
					System.out.print("ObjectID." + objectNames.get(object));
				}
				else
				{
					System.out.print("-1");
				}
				System.out.print(", ");
				if (npc != -1)
				{
					System.out.print("NpcID." + npcNames.get(npc));
				}
				else
				{
					System.out.print("-1");
				}
				System.out.print(", ");
				if (x != -1 && y != -1)
				{
					System.out.print("new WorldPoint(");
					System.out.print(x);
					System.out.print(", ");
					System.out.print(y);
					System.out.print(", 0)");
				}
				else
				{
					System.out.print("null");
				}
				System.out.print(", ");
				if (dstX != -1 && dstY != -1)
				{
					System.out.print("new WorldPoint(");
					System.out.print(dstX);
					System.out.print(", ");
					System.out.print(dstY);
					System.out.print(", 0)");
				}
				else
				{
					System.out.print("null");
				}
				System.out.print(", ");
				System.out.print(level);
				System.out.println("),");
			}
		}

		System.out.println("\t;");
		System.out.println();
		System.out.println("\tprivate final int taskId;");
		System.out.println("\tprivate final int completionVarb;");
		System.out.println("\tprivate final int objectId;");
		System.out.println("\tprivate final int npcId;");
		System.out.println("\tprivate final WorldPoint location;");
		System.out.println("\tprivate final WorldPoint destination;");
		System.out.println("\tprivate final int level;");
		System.out.println();
		System.out.println("\tpublic boolean isComplete(Client client)");
		System.out.println("\t{");
		System.out.println("\t\treturn client.getVarbitValue(getCompletionVarb()) != 0;");
		System.out.println("\t}");
		System.out.println();
		System.out.println("}");
	}

	private static Map<Integer, String> readConstants(String url) throws IOException
	{
		Map<Integer, String> values = new HashMap<>();
		try (Scanner scanner = new Scanner(Objects.requireNonNull(new URL(url).openStream())))
		{
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				if (!line.contains("=") || line.contains("*"))
				{
					continue;
				}

				String[] parts = line.split("=");
				String name = parts[0].trim()
					.substring("public static final int ".length());
				String value = parts[1].trim()
					.replace(";", "");

				values.put(Integer.parseInt(value), name);
			}
		}

		return values;
	}

}
