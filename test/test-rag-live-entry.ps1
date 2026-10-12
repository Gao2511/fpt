# Negative entry-point tests only. Approval and environment gates stop before Docker/network/database.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$oldMode=$env:RAG_ENVIRONMENT;$oldFlag=$env:RAG_ENABLED
$results=@()
try {
    $env:RAG_ENVIRONMENT='development';$env:RAG_ENABLED='false'
    $cases=@(
        @{Name='Missing explicit switch';Args=@{};Expected='Separate explicit live authorization'},
        @{Name='Actual unapproved plan';Args=@{ApprovedLivePlan=$true};Expected='Preflight rejected'},
        @{Name='Production environment';Args=@{ApprovedLivePlan=$true};Mode='production';Expected='Require development'},
        @{Name='Enabled production flag';Args=@{ApprovedLivePlan=$true};Flag='true';Expected='Require development'}
    )
    foreach($case in $cases){
        $env:RAG_ENVIRONMENT=if($case.Mode){$case.Mode}else{'development'}
        $env:RAG_ENABLED=if($case.Flag){$case.Flag}else{'false'}
        $blocked=$false;$argsForRunner=$case.Args
        try{& "$PSScriptRoot/run-rag-live.ps1" @argsForRunner | Out-Null}catch{$blocked=$_.Exception.Message.Contains($case.Expected)}
        if(!$blocked){throw "Entry gate failed: $($case.Name)"}
        $results+=@{case=$case.Name;result='PASS'};Write-Output "PASS $($case.Name)"
    }
    $tokens=$null;$errors=$null
    foreach($path in @('run-rag-live.ps1','rag-live-database.ps1','compile-rag-live-runner.ps1','test-rag-live-runner.ps1','test-rag-live-database.ps1')){
        [Management.Automation.Language.Parser]::ParseFile("$PSScriptRoot/$path",[ref]$tokens,[ref]$errors) | Out-Null
        if($errors.Count){throw "PowerShell syntax failed: $path"}
    }
    $report=Join-Path $root ('.build/phase241-entry-'+[guid]::NewGuid().ToString('N')+'.json')
    $file=[IO.File]::Open($report,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    try{$file.Write([Text.Encoding]::UTF8.GetBytes((@{passed=4;failed=0;realProviderRequests=0;databaseAccess=0;cases=$results}|ConvertTo-Json -Depth 5)));$file.Flush($true)}finally{$file.Dispose()}
    Write-Output "Entry tests 4/4; syntax 5/5; sanitized result: $report"
} finally {$env:RAG_ENVIRONMENT=$oldMode;$env:RAG_ENABLED=$oldFlag}
