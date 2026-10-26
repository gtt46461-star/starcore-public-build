from pathlib import Path
import subprocess,tempfile,shutil
base=Path(__file__).parents[1]
with tempfile.TemporaryDirectory(prefix='r25-java-') as temp:
    compiler=['javac'] if shutil.which('javac') else ['java','--module','jdk.compiler/com.sun.tools.javac.Main']
    subprocess.run(compiler+['-encoding','UTF-8','-d',temp,str(base/'code/LearningPolicyR25.java'),str(base/'tests/LearningPolicyR25Test.java')],check=True)
    subprocess.run(['java','-cp',temp,'com.ethk3.novaonepro.strategyv2.LearningPolicyR25Test'],check=True)
