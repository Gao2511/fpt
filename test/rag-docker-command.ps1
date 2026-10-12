# Bounded Docker client calls; never pass passwords in arguments or print private env files.
function Invoke-RagDocker {
    param([string[]]$DockerArgs,[int]$TimeoutSeconds=30)
    $info=[Diagnostics.ProcessStartInfo]::new()
    $info.FileName=(Get-Command docker -ErrorAction Stop).Source
    $info.UseShellExecute=$false;$info.CreateNoWindow=$true
    $info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    if(!$info.PSObject.Properties['ArgumentList']){throw 'Bounded Docker test runner requires PowerShell 7; use isolated local PostgreSQL option otherwise'}
    foreach($arg in $DockerArgs){$info.ArgumentList.Add($arg)}
    $process=[Diagnostics.Process]::new();$process.StartInfo=$info
    try {
        if(!$process.Start()){throw 'Docker client did not start'}
        $output=$process.StandardOutput.ReadToEndAsync();$errorOutput=$process.StandardError.ReadToEndAsync()
        if(!$process.WaitForExit($TimeoutSeconds*1000)){$process.Kill();$process.WaitForExit();throw 'Docker test command timed out; no further test actions will run'}
        if($process.ExitCode-ne 0){throw 'Docker test command failed; inspect Docker Desktop status privately'}
        $output.GetAwaiter().GetResult().Trim()
    } finally {$process.Dispose()}
}
