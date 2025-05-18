import subprocess

from typing import NamedTuple


class LoxCommandResult(NamedTuple):
    stdout: str
    stderr: str
    exit_code: int


def run_lox(lox, file):
    cmd = f"{lox} {file}"
    process = subprocess.run(
        cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE
    )
    out = process.stdout.decode()
    err = process.stderr.decode()

    return LoxCommandResult(
        stdout=out, stderr=err, exit_code=process.returncode
    )

def main():
    print(run_lox("java -jar C:/Users/Max/AppData/Local/Temp/codecrafters-build-dir/build-your-own-interpreter.jar run", "lox/function/nested_call_with_arguments.lox"))
    
if __name__ == '__main__':
    main()
