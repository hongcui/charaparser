use Encoding::FixLatin qw(fix_latin);
my $line=" corollas (3-)5-merous, ± actinomorphic or zygomorphic ";
$line = fix_latin($line);
print $line;
	
