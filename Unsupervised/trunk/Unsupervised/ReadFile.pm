package ReadFile;
use lib 'lib';
use Encoding::FixLatin qw(fix_latin);

sub readfile{
	my $file = shift;
	my $content = "";
	open(F, "$file") || die "$!:$file\n"; #audo decoding on read
	#open(F, '<:encoding(UTF-8)', "$file") || die "$!:$file\n"; #audo decoding on read
	while($line =<F>){
		$line = fix_latin($line);
		$line =~ s#\r|\n# #g;
		$content .= $line;
	}		 
	$content =~ s#\s+# #g;
	return $content;
}

1;