javac -d out $(find src -name "*.java")
java -cp out game.train.TrainerMain --preset fast


java -cp out game.train.TrainerMain --preset fast --gens 15 --episodeMs 8000


Windows 

# Compile all Java files
Get-ChildItem -Path src -Recurse -Filter "*.java" | ForEach-Object { javac -d out $_.FullName }

# Or compile them all at once
javac -d out @(Get-ChildItem -Path src -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName)

# Run the program
java -cp out game.train.TrainerMain --preset fast